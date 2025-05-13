package ch.sbb.matsim.umlego.writers.jdbc;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class VolumeCarpetRepository {

    private static final Logger LOG = LogManager.getLogger(VolumeCarpetRepository.class);

    public void insertEntries(Connection connection, List<BelastungsteppichEntry> entries) {
        String sql = "INSERT INTO volume_carpet " +
            "(RUN_ID, TARGET_DATE, TU_CODE, DEPARTURE_ID, TRAIN_NO, INDEX, ARRIVAL, DEPARTURE, TO_STOP_ARRIVAL, " +
            "FROM_STOP_NO, TO_STOP_NO, VOLUME, BOARDING, ALIGHTING, ORIGIN_BOARDING, DESTINATION_ALIGHTING) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            LOG.info("Preparing batch write query: {}", sql);

            for (BelastungsteppichEntry entry : entries) {
                fillStatement(entry, preparedStatement);

                preparedStatement.addBatch(); // Add to batch
            }

            int[] batchResults = preparedStatement.executeBatch(); // Execute batch
            LOG.info("Batch executed successfully, {} rows affected.", batchResults.length);
        } catch (Exception e) {
            LOG.error("Error inserting entries into volume_carpet: {}", e.getMessage(), e);
            throw new RuntimeException("Error inserting entries into volume_carpet", e);
        }
    }

    private static void fillStatement(BelastungsteppichEntry entry, PreparedStatement preparedStatement)
        throws SQLException {
        preparedStatement.setObject(1, entry.runId);
        preparedStatement.setObject(2, Date.valueOf(entry.targetDate));
        preparedStatement.setObject(3, entry.tuCode);
        preparedStatement.setObject(4, entry.departureId);
        preparedStatement.setObject(5, entry.trainNo);
        preparedStatement.setObject(6, entry.index);
        preparedStatement.setObject(7, entry.arrival);
        preparedStatement.setObject(8, entry.departure);
        preparedStatement.setObject(9, entry.toStopArrival);
        preparedStatement.setObject(10, entry.fromStopNo);
        preparedStatement.setObject(11, entry.toStopNo);
        preparedStatement.setObject(12, entry.volume);
        preparedStatement.setObject(13, entry.boarding);
        preparedStatement.setObject(14, entry.alighting);
        preparedStatement.setObject(15, entry.originBoarding);
        preparedStatement.setObject(16, entry.destinationAlighting);
    }

    public record BelastungsteppichEntry(
        String runId,
        LocalDate targetDate,
        String tuCode,
        Integer departureId,
        Integer trainNo,
        Integer index,
        String arrival,
        String departure,
        String toStopArrival,
        Integer fromStopNo,
        Integer toStopNo,
        Double volume,
        Double boarding,
        Double alighting,
        Double originBoarding,
        Double destinationAlighting) {

    }
}
