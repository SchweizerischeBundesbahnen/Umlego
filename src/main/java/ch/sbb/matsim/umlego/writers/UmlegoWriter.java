package ch.sbb.matsim.umlego.writers;

import static ch.sbb.matsim.umlego.config.UmlegoConfig.isRunningLocally;
import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.connect;
import static ch.sbb.matsim.umlego.util.PathUtil.ensureDir;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;
import ch.sbb.matsim.umlego.Umlego.WriterParameters;
import ch.sbb.matsim.umlego.UmlegoWorker.WorkResult;
import ch.sbb.matsim.umlego.config.UmlegoWriterType;
import ch.sbb.matsim.umlego.demand.UnroutableDemand;
import ch.sbb.matsim.umlego.writers.jdbc.JdbcGlobalStatsWriter;
import ch.sbb.matsim.umlego.writers.jdbc.JdbcSkimWriter;
import ch.sbb.matsim.umlego.writers.jdbc.JdbcVolumeCarpetWriter;
import ch.sbb.matsim.umlego.writers.types.skim.Skim;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UmlegoWriter implements Runnable {

    private static final Logger LOG = LogManager.getLogger(UmlegoWriter.class);

    private final BlockingQueue<Future<WorkResult>> queue;
    private final String outputFolder;
    private final List<String> originZoneIds;
    private final List<String> destinationZoneIds;
    private final WriterParameters params;
    private final String runId;
    private final LocalDate targetDate;
    private final CompletableFuture<UnroutableDemand> futureUnroutableDemand = new CompletableFuture<>();
    private final CompletableFuture<Skim> futureSkim = new CompletableFuture<>();
    private final CompletableFuture<Map<String, Double>> futureGlobalStats = new CompletableFuture<>();

    public UmlegoWriter(BlockingQueue<Future<WorkResult>> queue, String outputFolder, List<String> originZoneIds,
        List<String> destinationZoneIds, WriterParameters params, String runId, LocalDate targetDate) {
        this.queue = queue;
        this.outputFolder = outputFolder;
        this.originZoneIds = originZoneIds;
        this.destinationZoneIds = destinationZoneIds;
        this.params = params;
        this.runId = runId;
        this.targetDate = targetDate;
    }

    @Override
    public void run() {
        UnroutableDemand unroutableDemand = writeRoutes();
        this.futureUnroutableDemand.complete(unroutableDemand);
    }

    private UmlegoWriterInterface getWriter(UmlegoWriterType type) throws IOException {

        ensureDir(outputFolder);

        return switch (type) {
            case BLP -> createBlpWriter(runId);
            case SKIM -> createSkimWriter(runId);
            case STATS_JDBC -> createGlobalStatsJdbcWriter(runId);
            case STATS_FILE -> createGlobalStatsFileWriter();
            case CSV -> new UmlegoCsvWriter(Paths.get(this.outputFolder, "connections.csv.gz").toString(), true);
            case PutSurvey -> new PutSurveyWriter(Paths.get(this.outputFolder, "visum.net.gz").toString());
        };
    }

    private UmlegoWriterInterface createBlpWriter(String runId) {
        if (runId != null) {
            return new JdbcVolumeCarpetWriter(connect(), runId, targetDate, params.schedule());
        } else {
            return new UmlegoBlpWriter(Paths.get(this.outputFolder, "belastungsteppich.csv.gz").toString(),
                params.schedule());
        }
    }

    private UmlegoWriterInterface createSkimWriter(String runId) {
        if (!isRunningLocally()) {
            return new JdbcSkimWriter(connect(), runId, targetDate);
        } else {
            return new UmlegoSkimWriter(Paths.get(this.outputFolder, "skims.csv.gz").toString());
        }
    }

    private UmlegoWriterInterface createGlobalStatsJdbcWriter(String runId) {
        return new JdbcGlobalStatsWriter(connect(), runId, targetDate);
    }

    private UmlegoWriterInterface createGlobalStatsFileWriter() {
        return new GlobalStatsWriter(Paths.get(this.outputFolder, "stats.json").toString());
    }

    private UmlegoWriterInterface createWriter(Supplier<UmlegoWriterInterface> dbWriterSupplier,
        Supplier<UmlegoWriterInterface> fileWriterSupplier) {
        try {
            if (!isRunningLocally()) {
                return dbWriterSupplier.get();
            } else {
                return fileWriterSupplier.get();
            }
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to create writer", e);
        }
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
                        for (var listener : params.listeners()) {
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

    public Skim getSkim() {
        try {
            LOG.info("Retrieving global stats information from JdbcSkimWriter");
            return futureSkim.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to retrieve final skim", e);
        }
    }

    public Map<String, Double> getGlobalStats() {
        try {
            LOG.info("Retrieving global stats information from JdbcGlobalStatsWriter");
            return this.futureGlobalStats.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Failed to retrieve final global statistics", e);
        }
    }

    public class UmlegoWriters implements AutoCloseable {

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
                    if (w instanceof JdbcSkimWriter) {
                        Skim skim = ((JdbcSkimWriter) w).getSkims();
                        LOG.info("Completed futureSkim: " + skim);
                        futureSkim.complete(skim);
                    } else if (w instanceof JdbcGlobalStatsWriter) {
                        Map<String, Double> globalStats = ((JdbcGlobalStatsWriter) w).getGlobalStats();
                        LOG.info("Completed futureGlobalStats: " + globalStats);
                        futureGlobalStats.complete(globalStats);
                    }
                } catch (Exception exception) {
                    throw new RuntimeException(exception);
                }
            });
            if (!futureSkim.isDone()) {
                futureSkim.completeExceptionally(new RuntimeException("Skim not completed"));
            }
            if (!futureGlobalStats.isDone()) {
                futureGlobalStats.completeExceptionally(new RuntimeException("GlobalStats not completed"));
            }
        }
    }
}
