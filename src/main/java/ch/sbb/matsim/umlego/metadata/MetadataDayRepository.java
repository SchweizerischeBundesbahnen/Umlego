package ch.sbb.matsim.umlego.metadata;

import static ch.sbb.matsim.umlego.metadata.MetadataDayKey.VALID_AUTO;
import static ch.sbb.matsim.umlego.util.SnowflakeUtil.read;
import static ch.sbb.matsim.umlego.util.SnowflakeUtil.write;

import ch.sbb.matsim.umlego.util.RunId;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * MetadataDayRepository provides methods for reading and writing specific day metadata for umlego.
 */
public final class MetadataDayRepository {

    /**
     * Inserts a record into the metadata-day table.
     *
     * @param connection the database connection to use
     * @param runId the run identifier
     * @param targetDate the target date in String format dd.MM.yyyy
     * @param key the metadata key
     * @param value the metadata value
     */
    public void insertMetadata(Connection connection, String runId, LocalDate targetDate, String key, Object value) {
        Date convertedTargetDate = Date.valueOf(targetDate);
        String query = "INSERT INTO metadata_day (run_id, target_date, key, value) VALUES (?, ?, ?, ?)";
        if (value == null){
            write(connection, query, runId, convertedTargetDate, key, null);
        } else {
            write(connection, query, runId, convertedTargetDate, key, String.valueOf(value));
        }
    }

    /**
     * Inserts a record into the metadata-day table.
     *
     * @param connection the database connection to use
     * @param runId the run identifier
     * @param targetDate the target date in String format dd.MM.yyyy
     * @param key the metadata key
     * @param value the metadata value
     */
    public void insertMetadata(Connection connection, String runId, LocalDate targetDate, MetadataDayKey key,
            Object value) {
        insertMetadata(connection, runId, targetDate, key.toString(), value);
    }

    /**
     * Checks if the following combination is already existing in the Metadata: (TARGET_DATE, SAISON_BASE_ID,
     * TIMETABLE).
     *
     * @param connection JDBC connection
     * @param targetDate day for Umlego
     * @param saisonBase saison run id (only base part)
     * @param timetable timetable filename
     */
    public boolean findByTargetDateSaisonBaseTimetable(Connection connection, LocalDate targetDate, String saisonBase,
            String timetable) {
        String query = """
                SELECT *
                  FROM metadata md1
                  JOIN metadata md2 ON md1.run_id = md2.run_id
                  LEFT JOIN metadata_day mdd ON md1.run_id = mdd.run_id
                 WHERE md1.key = 'TIMETABLE' AND md1.value = ?
                   AND md2.key = 'SAISON_BASE_ID' AND md2.value = ?
                   AND mdd.target_date = ?
                   AND mdd.key = 'VALID_AUTO' AND mdd.value = 'true';
                """;
        try (ResultSet resultSet = read(connection, query, timetable, saisonBase, targetDate.toString())) {
            return resultSet.next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Checks if the entry of the metadata_day table for the given Day-Umlego is valid.
     *
     * @param connection the database connection to use
     * @param runId the run identifier
     * @param targetDate the target date in String format dd.MM.yyyy
     */
    public boolean isUmlegoValid(Connection connection, RunId runId, LocalDate targetDate) {
        boolean isValid = false;
        String query = "SELECT value FROM metadata_day WHERE run_id = ? AND target_date = ? AND key = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, runId.getValue());
            ps.setDate(2, Date.valueOf(targetDate));
            ps.setString(3, String.valueOf(VALID_AUTO));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String result = rs.getString("value").trim();
                    isValid = Boolean.parseBoolean(result);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error while getting the UmlegoValidation value from the metadata_day table", e);
        }
        return isValid;
    }

    /**
     * Gets a comma-separated list of target dates where valid_auto = false for the given RunId.
     *
     * @param connection database connection
     * @param runId the RunId to check
     * @return comma-separated list of target dates, empty string if none found
     */
    public String getInvalidTargetDates(Connection connection, String runId) {
        String query = """
                   SELECT target_date FROM metadata_day
                   WHERE run_id = ? AND key = 'VALID_AUTO' AND value = 'false'
                   ORDER BY target_date
                   """;
        try {
            ResultSet resultSet = read(connection, query, runId);
            StringBuilder dates = new StringBuilder();
            while (resultSet.next()) {
                if (!dates.isEmpty()) {
                    dates.append(", ");
                }
                dates.append(resultSet.getDate("target_date").toLocalDate().toString());
            }
            return dates.toString();
        } catch (SQLException e) {
            throw new RuntimeException("Error getting invalid target dates: " + e.getMessage(), e);
        }
    }
}
