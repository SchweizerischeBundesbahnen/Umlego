package ch.sbb.matsim.umlego.writers.jdbc;

import java.sql.Date;
import java.time.LocalDate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class SkimRepository {

    private static final Logger LOG = LogManager.getLogger(SkimRepository.class);

    public void insertEntries(Connection connection, List<SkimEntry> entries) {
        String sql = "INSERT INTO skim " +
            "(RUN_ID, TARGET_DATE, ORIGIN, DESTINATION, DEMAND, ROUTE_COUNT, TOTAL_JOURNEY_TIME, " +
            "JOURNEY_TIME_WEIGHTED, TRANSFER_COUNT_WEIGHTED, ADAPTATION_TIME_WEIGHTED) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            LOG.info("Preparing batch write query: {}", sql);
            for (SkimEntry entry : entries) {
                fillStatement(entry, preparedStatement);
                preparedStatement.addBatch();
            }
            int[] batchResults = preparedStatement.executeBatch();
            LOG.info("Batch executed successfully, {} rows affected.", batchResults.length);
        } catch (SQLException e) {
            LOG.error("Error inserting entries into skim: {}", e.getMessage(), e);
            throw new RuntimeException("Error inserting entries into skim", e);
        }
    }

    private static void fillStatement(SkimEntry entry, PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.setObject(1, entry.runId);
        preparedStatement.setObject(2, Date.valueOf(entry.targetDate));
        preparedStatement.setObject(3, entry.origin);
        preparedStatement.setObject(4, entry.destination);
        preparedStatement.setObject(5, entry.demand);
        preparedStatement.setObject(6, entry.routeCount);
        preparedStatement.setObject(7, entry.totalJourneyTime);
        preparedStatement.setObject(8, entry.journeyTimeWeighted);
        preparedStatement.setObject(9, entry.transferCountWeighted);
        preparedStatement.setObject(10, entry.adaptationTimeWeighted);
    }

    public record SkimEntry(
        String runId,
        LocalDate targetDate,
        Integer origin,
        Integer destination,
        Double demand,
        Integer routeCount,
        Integer totalJourneyTime,
        Double journeyTimeWeighted,
        Double transferCountWeighted,
        Double adaptationTimeWeighted) {

    }
}
