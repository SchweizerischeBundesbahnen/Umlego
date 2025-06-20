package ch.sbb.matsim.umlego.writers;

import ch.sbb.matsim.umlego.FoundRoute;
import ch.sbb.matsim.umlego.UmlegoListener;
import ch.sbb.matsim.umlego.UmlegoWorkResult;
import ch.sbb.matsim.umlego.WorkResultHandler;
import ch.sbb.matsim.umlego.config.UmlegoWriterType;
import ch.sbb.matsim.umlego.config.WriterParameters;
import ch.sbb.matsim.umlego.demand.UnroutableDemand;
import ch.sbb.matsim.umlego.demand.UnroutableDemandWriter;
import ch.sbb.matsim.umlego.demand.UnroutableDemandWriterFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static ch.sbb.matsim.umlego.util.PathUtil.ensureDir;

/**
 * Handler that writes {@link UmlegoWorkResult} to the configured writers and notifies listeners about the results.
 */
public class ResultWriter implements WorkResultHandler<UmlegoWorkResult> {

    private final String outputFolder;
    private final TransitSchedule schedule;
    private final List<UmlegoListener> listeners;
    private final List<UmlegoWriter> writers;
    private final WriterParameters params;
    private final List<String> destinationZoneIds;
    private final UnroutableDemand unroutableDemand = new UnroutableDemand();

    /**
     * Creates a BufferedWriter for the given filename. Supports .gz compressed output.
     */
    public static BufferedWriter newBufferedWriter(String filename) throws IOException {

        Path path = Paths.get(filename);
        if (filename.endsWith(".gz"))
            return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path))));

        return Files.newBufferedWriter(path);
    }

    public ResultWriter(String outputFolder, TransitSchedule schedule,
                        List<UmlegoListener> listeners,
                        WriterParameters params, List<String> destinationZoneIds) {
        ensureDir(outputFolder);
        this.outputFolder = outputFolder;
        this.schedule = schedule;
        this.listeners = listeners;
        this.params = params;
        this.destinationZoneIds = destinationZoneIds;
        this.writers = params.writerTypes().stream().map(this::getWriter).toList();
    }

    private UmlegoWriter getWriter(UmlegoWriterType type) {
        return switch (type) {
            case BLP ->
                    new UmlegoBlpWriter(Paths.get(this.outputFolder, "belastungsteppich.csv.gz").toString(), schedule);
            case SKIM -> new UmlegoSkimWriter(Paths.get(this.outputFolder, "skims.csv.gz").toString());
            case CSV -> new UmlegoCsvWriter(Paths.get(this.outputFolder, "connections.csv.gz").toString(), true);
            case PutSurvey -> new PutSurveyWriter(Paths.get(this.outputFolder, "visum.net.gz").toString());
        };
    }

    public UnroutableDemand getUnroutableDemand() {
        return unroutableDemand;
    }

    @Override
    public void handleResult(UmlegoWorkResult result) {

        unroutableDemand.getParts().addAll(result.unroutableDemand().getParts());
        String origZone = result.originZone();
        Map<String, List<FoundRoute>> routesPerDestination = result.routesPerDestinationZone();
        if (routesPerDestination == null) {
            // looks like this zone cannot reach any destination
            return;
        }

        for (String destZone : destinationZoneIds) {
            if (origZone.equals(destZone)) {
                // we're not interested in intrazonal trips
                continue;
            }
            List<FoundRoute> routesToDestination = routesPerDestination.get(destZone);
            if (routesToDestination == null || routesToDestination.isEmpty()) {
                // looks like there are no routes to this destination from the given origin zone
                continue;
            }

            for (FoundRoute route : routesToDestination) {
                for (var listener : listeners) {
                    listener.processRoute(origZone, destZone, route);
                }

                if (route.demand >= this.params.minimalDemandForWriting()) {
                    writers.forEach(w -> w.writeRoute(origZone, destZone, route));
                }
            }

            for (UmlegoListener listener : listeners) {
                listener.processODPair(origZone, destZone);
            }

            writers.forEach(w -> w.writeResult(result, destZone));
        }

    }

    @Override
    public void close() throws Exception {
        // close all listeners
        listeners.forEach(UmlegoListener::finish);

        for (UmlegoWriter w : writers) {
            w.close();
        }

        UnroutableDemandWriter demandWriter = UnroutableDemandWriterFactory.createWriter(outputFolder);
        demandWriter.write(unroutableDemand);
    }
}
