package ch.sbb.matsim.umlego.writers;

import ch.sbb.matsim.umlego.config.hadoop.FileSystemUtil;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

public class GlobalStatsWriter extends AbstractGlobalStatsWriter {

	private static final Logger LOG = LogManager.getLogger(GlobalStatsWriter.class);

	private static final String OVERALL_KEY = "Overall";
	private static final String AVERAGE_WEIGHTED_ADAPTION_TIME_KEY = "AverageWeightedAdaptationTime";
	private static final String AVERAGE_WEIGHTED_TRANSFERS_KEY = "AverageWeightedTransfers";
	public static final String TOTAL_DEMAND_KEY = "TotalDemand";
	private static final int INDENT_FACTOR = 4;

	private final String filename;

	public GlobalStatsWriter(String filename) {
		this.filename = filename;
	}

	@Override
	public void close() throws Exception {
		Map<String, Map<String, Double>> globalStats = new HashMap<>();
		for (Map.Entry<String, StatisticsAccumulator> entry : statsMap.entrySet()) {
			String key = entry.getKey();
			StatisticsAccumulator accumulator = entry.getValue();
			globalStats.put(key, accumulator.calculateGlobalStats());
		}

		globalStats.put(OVERALL_KEY, Map.of(
			AVERAGE_WEIGHTED_ADAPTION_TIME_KEY, totalWeightedAdaptationTime / totalDemand,
			AVERAGE_WEIGHTED_TRANSFERS_KEY, totalWeightedTransfers / totalDemand,
			TOTAL_DEMAND_KEY, totalDemand
		));

		writeStatsToJson(globalStats);
	}

	private void writeStatsToJson(Map<String, Map<String, Double>> stats) {
		JSONObject jsonObject = new JSONObject(stats);
		try (BufferedWriter writer = FileSystemUtil.getBufferedWriter(filename)) {
			writer.write(jsonObject.toString(INDENT_FACTOR));
		} catch (IOException e) {
			LOG.error("Failed to write global stats to JSON", e);
			throw new RuntimeException("Failed to write global stats to JSON", e);
		}
	}
}
