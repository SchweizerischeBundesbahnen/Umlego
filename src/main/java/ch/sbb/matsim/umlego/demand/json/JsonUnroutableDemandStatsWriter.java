package ch.sbb.matsim.umlego.demand.json;

import ch.sbb.matsim.umlego.demand.UnroutableDemandStats;
import ch.sbb.matsim.umlego.demand.UnroutableDemandZone;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.writers.UnroutableDemandStatsWriter;
import java.io.FileWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Paths;

public class JsonUnroutableDemandStatsWriter implements UnroutableDemandStatsWriter {

    private static final Logger LOG = LogManager.getLogger(JsonUnroutableDemandStatsWriter.class);
    private static final String FILENAME = "unroutable_stats.json";
    private static final int INDENT_FACTOR = 4;

    private final String outputPath;

    public JsonUnroutableDemandStatsWriter(String outputPath) {
        this.outputPath = outputPath;
    }

    @Override
    public void write(UnroutableDemandStats stats) {
        LOG.info("Writing unroutable demand metadata to JSON");

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getOutputFilePath()))) {
            JSONObject json = new JSONObject();
            UnroutableDemandZone largestZone = stats.largestUnroutableDemandZone();
            
            json.put("UnroutableDemand", stats.totalUnroutableDemand());
            json.put("ShareUnroutableDemand", stats.percentUnroutableDemand());
            json.put("LargestUnroutableZone", largestZone != null && largestZone.fromZone() != null ? largestZone.fromZone() : "");
            json.put("DemandLargestUnroutableZone", largestZone != null && largestZone.demand() != null ? largestZone.demand() : 0.0);
            
            writer.write(json.toString(INDENT_FACTOR));
        } catch (IOException | ZoneNotFoundException e) {
            LOG.error("Failed to write unroutable demand metadata to JSON", e);
            throw new RuntimeException("Failed to write unroutable demand metadata to JSON", e);
        }
    }

    private String getOutputFilePath() {
        return Paths.get(outputPath, FILENAME).toString();
    }
} 