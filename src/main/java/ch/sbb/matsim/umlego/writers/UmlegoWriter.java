package ch.sbb.matsim.umlego.writers;

import static ch.sbb.matsim.umlego.util.PathUtil.ensureDir;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;
import ch.sbb.matsim.umlego.UmlegoSkimCalculator;
import ch.sbb.matsim.umlego.config.WriterParameters;
import ch.sbb.matsim.umlego.UmlegoListener;
import ch.sbb.matsim.umlego.UmlegoWorker.WorkResult;
import ch.sbb.matsim.umlego.config.UmlegoWriterType;
import ch.sbb.matsim.umlego.demand.UnroutableDemand;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class UmlegoWriter implements Runnable {

    private static final Logger LOG = LogManager.getLogger(UmlegoWriter.class);

    private final BlockingQueue<Future<WorkResult>> queue;
    private final String outputFolder;
    private final List<String> originZoneIds;
    private final List<String> destinationZoneIds;
    private final List<UmlegoListener> listeners;
    private final TransitSchedule schedule;
    private final WriterParameters params;
    private final CompletableFuture<UnroutableDemand> futureUnroutableDemand = new CompletableFuture<>();

    public UmlegoWriter(BlockingQueue<Future<WorkResult>> queue,
                        String outputFolder, List<String> originZoneIds,
                        List<String> destinationZoneIds,
                        List<UmlegoListener> listeners,
                        TransitSchedule schedule,
                        WriterParameters params) {
        this.queue = queue;
        this.outputFolder = outputFolder;
        this.originZoneIds = originZoneIds;
        this.destinationZoneIds = destinationZoneIds;
        this.listeners = listeners;
        this.schedule = schedule;
        this.params = params;
    }

    /**
     * Creates a BufferedWriter for the given filename. Supports .gz compressed output.
     */
    public static BufferedWriter newBufferedWriter(String filename) throws IOException {

        Path path = Paths.get(filename);
        if (filename.endsWith(".gz"))
            return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path))));

        return Files.newBufferedWriter(path);
    }

    @Override
    public void run() {
        UnroutableDemand unroutableDemand = writeRoutes();

        // close all listeners
        listeners.forEach(UmlegoListener::finish);

        this.futureUnroutableDemand.complete(unroutableDemand);
    }

    private UmlegoWriterInterface getWriter(UmlegoWriterType type) throws IOException {

        ensureDir(outputFolder);

        return switch (type) {
            case BLP -> createBlpWriter();
            case SKIM -> createSkimWriter();
            case CSV -> new UmlegoCsvWriter(Paths.get(this.outputFolder, "connections.csv.gz").toString(), true);
            case PutSurvey -> new PutSurveyWriter(Paths.get(this.outputFolder, "visum.net.gz").toString());
        };
    }

    private UmlegoWriterInterface createBlpWriter() {
        return new UmlegoBlpWriter(Paths.get(this.outputFolder, "belastungsteppich.csv.gz").toString(), schedule);
    }

    private UmlegoWriterInterface createSkimWriter() {
        Optional<UmlegoSkimCalculator> calc = this.listeners.stream()
                .filter(s -> s instanceof UmlegoSkimCalculator)
                .map(UmlegoSkimCalculator.class::cast)
                .findFirst();

        return new UmlegoSkimWriter(calc.orElseThrow(), Paths.get(this.outputFolder, "skims.csv.gz").toString());
    }


    private UmlegoWriters getWriters() throws IOException {
        var writers = params.writerTypes().stream().map(type -> {
            try {
                return this.getWriter(type);
            } catch (IOException exception) {
                throw new UncheckedIOException(exception);
            }

        }).collect(Collectors.toSet());
        return new UmlegoWriters(writers);
    }

    private UnroutableDemand writeRoutes() {
        LOG.info("writing output to {}", this.outputFolder);
        UnroutableDemand unroutableDemand = new UnroutableDemand();
        int totalItems = this.originZoneIds.size();
        int counter = 0;

        try (var writers = this.getWriters()) {
            while (true) {
                Future<WorkResult> futureResult = this.queue.take();
                WorkResult result = futureResult.get();
                if (result.originZone() == null) {
                    // end marker, the work is done
                    break;
                }

                counter++;
                LOG.info(" - writing routes starting in zone {} ({}/{})", result.originZone(), counter, totalItems);
                unroutableDemand.getParts().addAll(result.unroutableDemand().getParts());
                String origZone = result.originZone();
                Map<String, List<FoundRoute>> routesPerDestination = result.routesPerDestinationZone();
                if (routesPerDestination == null) {
                    // looks like this zone cannot reach any destination
                    continue;
                }
                for (String destZone : destinationZoneIds) {
                    if (origZone.equals(destZone)) {
                        // we're not interested in intrazonal trips
                        continue;
                    }
                    List<FoundRoute> routesToDestination = routesPerDestination.get(destZone);
                    if (routesToDestination == null) {
                        // looks like there are no routes to this destination from the given origin zone
                        continue;
                    }

                    for (FoundRoute route : routesToDestination) {
                        for (var listener : listeners) {
                            listener.processRoute(origZone, destZone, route);
                        }

                        if (route.demand.getDouble(destZone) >= this.params.minimalDemandForWriting()) {
                            writers.writeRoute(origZone, destZone, route);
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return unroutableDemand;
    }

    /**
     * Once the unroutable demand becomes available, the calculation and writing has finished
     *
     * @return UnroutableDemand
     */
    public UnroutableDemand getUnroutableDemand() {
        try {
            return futureUnroutableDemand.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public static class UmlegoWriters implements AutoCloseable {

        Set<UmlegoWriterInterface> writers;

        public UmlegoWriters(Set<UmlegoWriterInterface> writers) {
            this.writers = writers;
        }

        public void writeRoute(String origZone, String destZone, FoundRoute route) {
            for (var writer : writers) {
                writer.writeRoute(origZone, destZone, route);
            }
        }

        @Override
        public void close() throws Exception {
            this.writers.forEach(w -> {
                try {
                    w.close();
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
    }
}
