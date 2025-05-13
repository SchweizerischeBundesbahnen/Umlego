package ch.sbb.matsim.umlego.writers.jdbc;

import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.closeConnection;
import static ch.sbb.matsim.umlego.metadata.MetadataDayKey.ADAPTATION_TIME_WEIGHTED;
import static ch.sbb.matsim.umlego.metadata.MetadataDayKey.PERSON_HOURS;
import static ch.sbb.matsim.umlego.metadata.MetadataDayKey.PERSON_KM;
import static ch.sbb.matsim.umlego.metadata.MetadataDayKey.ROUTED_DEMAND;
import static ch.sbb.matsim.umlego.metadata.MetadataDayKey.TRANSFERS_WEIGHTED;

import ch.sbb.matsim.umlego.metadata.MetadataDayRepository;
import ch.sbb.matsim.umlego.writers.AbstractGlobalStatsWriter;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The {@link JdbcGlobalStatsWriter} class is responsible for generating global statistics and writing them
 * to a metadata repository using a JDBC connection. It extends {@link AbstractGlobalStatsWriter}.
 */
public class JdbcGlobalStatsWriter extends AbstractGlobalStatsWriter {

    private static final Logger LOG = LogManager.getLogger(JdbcGlobalStatsWriter.class);

    private final Connection connection;
    private final LocalDate targetDate;
    private final String runId;
    private Map<String, Double> globalStats;

    /**
     * Constructs a new {@link JdbcGlobalStatsWriter}.
     *
     * @param connection the database connection to use
     * @param runId the run identifier
     * @param targetDate the target date for which the stats are being written
     */
    public JdbcGlobalStatsWriter(Connection connection, String runId, LocalDate targetDate) {
        this.connection = connection;
        this.runId = runId;
        this.targetDate = targetDate;
        LOG.info("Initialized JdbcGlobalStatsWriter with runId: {}, targetDate: {}", runId, targetDate);
    }

    /**
     * Finalizes the writing process by generating the stats map and storing it in the metadata repository.
     *
     * @throws Exception if an error occurs during the writing process
     */
    @Override
    public void close() {
        LOG.info("Closing JdbcGlobalStatsWriter and writing global stats metadata.");
        try {
            createStatsMap(statsMap);
            LOG.debug("Created stats map: {}", globalStats);
            writeGlobalStatsMetadata(connection, runId, targetDate);
            closeConnection(connection);
            LOG.info("Successfully wrote global stats metadata for runId: {}, targetDate: {}", runId, targetDate);
        } catch (RuntimeException e) {
            throw new RuntimeException("Error while closing JdbcGlobalStatsWriter", e);
        }
    }

    /**
     * Retrieves the global statistics calculated during the writing process.
     *
     * @return a map of global statistics, where the key is the stat name and the value is its value
     */
    public Map<String, Double> getGlobalStats() {
        LOG.debug("Retrieving global stats: {}", globalStats);
        return this.globalStats;
    }

    /**
     * Creates a map of global statistics based on the provided statistics accumulators.
     *
     * @param statsMap a map of statistics accumulators, grouped by a specific key
     * @return a map of global statistics, where the key is the stat name and the value is its value
     */
    void createStatsMap(Map<String, StatisticsAccumulator> statsMap) {
        LOG.info("Creating stats map from statistics.");
        globalStats = new HashMap<>();
        for (Map.Entry<String, StatisticsAccumulator> entry : statsMap.entrySet()) {
            String key = entry.getKey();
            globalStats.put(PERSON_KM + "_" + key, entry.getValue().totalPersonKm);
            globalStats.put(PERSON_HOURS + "_" + key, entry.getValue().totalPersonHours);
        }
        globalStats.put(ADAPTATION_TIME_WEIGHTED.toString(), totalWeightedAdaptationTime / totalDemand);
        globalStats.put(TRANSFERS_WEIGHTED.toString(), totalWeightedTransfers / totalDemand);
        globalStats.put(ROUTED_DEMAND.toString(), totalDemand);


        LOG.debug("Stats map created: {}", globalStats);
    }

    /**
     * Writes the global statistics metadata to the repository.
     *
     * @param connection the database connection to use
     * @param runId the run identifier
     * @param targetDate the target date for which the stats are being written
     */
    protected void writeGlobalStatsMetadata(
            Connection connection,
            String runId,
            LocalDate targetDate) {
        LOG.info("Writing global stats metadata to the repository.");
        MetadataDayRepository repo = new MetadataDayRepository();

        for (Map.Entry<String, Double> stat : globalStats.entrySet()) {
            LOG.debug("Writing stat: key={}, value={}", stat.getKey(), stat.getValue());
            repo.insertMetadata(connection, runId, targetDate, stat.getKey(), stat.getValue());
        }
        LOG.info("Finished writing global stats metadata.");
    }
}
