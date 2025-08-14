package ch.sbb.matsim.umlego.writers;

import ch.sbb.matsim.umlego.UmlegoWorkResult;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import ch.sbb.matsim.umlego.FoundRoute;
import ch.sbb.matsim.umlego.UmlegoListener;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkResultHandler;
import ch.sbb.matsim.umlego.config.CompressionType;
import ch.sbb.matsim.umlego.config.UmlegoWriterType;
import ch.sbb.matsim.umlego.config.WriterParameters;
import ch.sbb.matsim.umlego.demand.UnroutableDemand;
import ch.sbb.matsim.umlego.demand.UnroutableDemandWriter;
import ch.sbb.matsim.umlego.demand.UnroutableDemandWriterFactory;
import static ch.sbb.matsim.umlego.util.PathUtil.ensureDir;

/**
 * Handler that writes {@link UmlegoWorkResult} to the configured writers and notifies listeners about the results.
 */
public class ResultWriter implements WorkResultHandler<UmlegoWorkResult> {

    private final String outputFolder;
    private final TransitSchedule schedule;
    private final List<UmlegoListener> listeners;
    private final WriterParameters params;
    private final List<String> destinationZoneIds;
    private final UnroutableDemand unroutableDemand = new UnroutableDemand();

    /**
     * Get the file name for the desired compression type.
     */
    public static String getFilename(String folder, String filename, CompressionType type) {
        return switch (type) {
            case GZIP -> Paths.get(folder, filename + ".gz").toString();
            case ZSTD -> Paths.get(folder, filename + ".zst").toString();
            case NONE -> Paths.get(folder, filename).toString();
        };
    }

    /**
     * Creates a BufferedWriter for the given filename, automatically detecting required compression based on the file extension.
     */
    public static BufferedWriter newBufferedWriter(String filename) throws IOException {

        Path path = Paths.get(filename);
        if (filename.endsWith(".gz"))
            return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(path))));

        if (filename.endsWith(".zst"))
            return new BufferedWriter(new OutputStreamWriter(new ZstdCompressorOutputStream(Files.newOutputStream(path))));

        return Files.newBufferedWriter(path);
    }

    public ResultWriter(String outputFolder, TransitSchedule schedule,
                        List<UmlegoListener> listeners,
                        WriterParameters params, List<String> destinationZoneIds) {
        ensureDir(outputFolder);
        this.outputFolder = outputFolder;
        this.schedule = schedule;
        this.params = params;
        this.destinationZoneIds = destinationZoneIds;
        
        // Combine external listeners with configured writers
        List<UmlegoListener> writers = params.writerTypes().stream().map(this::getWriter).toList();
        this.listeners = new ArrayList<>();
        this.listeners.addAll(listeners);
        this.listeners.addAll(writers);
    }

    private UmlegoListener getWriter(UmlegoWriterType type) {
        return switch (type) {
            case BLP ->
                    new UmlegoBlpWriter(getFilename(this.outputFolder, "belastungsteppich.csv", params.compression()), params, schedule);
            case SKIM -> new UmlegoSkimWriter(getFilename(this.outputFolder, "skims.csv", params.compression()), params);
            case CSV ->
                    new UmlegoCsvWriter(getFilename(this.outputFolder, "connections.csv", params.compression()), true, params);
            case PutSurvey -> new PutSurveyWriter(getFilename(this.outputFolder, "visum.net", params.compression()), params);
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
            }

            for (UmlegoListener listener : listeners) {
                listener.processODPair(origZone, destZone);
            }

            listeners.forEach(l -> l.processResult(result, destZone));
        }

    }

    @Override
    public void close() throws Exception {
        // close all listeners (including writers)
        for (UmlegoListener listener : listeners) {
            listener.finish();
        }

        UnroutableDemandWriter demandWriter = UnroutableDemandWriterFactory.createWriter(outputFolder);
        demandWriter.write(unroutableDemand);
    }
}
