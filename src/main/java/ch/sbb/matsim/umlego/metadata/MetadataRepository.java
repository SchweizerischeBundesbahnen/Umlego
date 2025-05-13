package ch.sbb.matsim.umlego.metadata;

import static ch.sbb.matsim.umlego.metadata.MetadataKey.SAISON_BASE_ID;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.TIMETABLE;
import static ch.sbb.matsim.umlego.util.SnowflakeUtil.read;
import static ch.sbb.matsim.umlego.util.SnowflakeUtil.write;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MetadataRepository provides methods for reading and writing metadata for umlego.
 */
public final class MetadataRepository {

    static Logger LOG = LogManager.getLogger(MetadataRepository.class);

    /**
     * Inserts a record into the metadata table.
     *
     * @param connection the database connection to use
     * @param runId the run identifier
     * @param key the metadata key
     * @param value the metadata value
     */
    public void insertMetadata(Connection connection, String runId, MetadataKey key, Object value) {
        String query = "INSERT INTO metadata (run_id, key, value) VALUES (?, ?, ?)";
        write(connection, query, runId, key.toString(), String.valueOf(value));
    }

    /**
     * Executes a query to read the already readed filenames from the metadata-day table.
     *
     * @param connection the database connection to use
     * @return List containing the filenames found in the metadata table
     */
    public List<String> findAllProcessedTimetables(Connection connection) {
        String query = "SELECT distinct value FROM metadata WHERE key = '" + TIMETABLE + "'";
        LOG.debug("Executing query: {}", query);

        try (ResultSet resultSet = read(connection, query)) {
            List<String> filesList = new ArrayList<>();
            while (resultSet.next()) {
                filesList.add(resultSet.getString("value"));
            }
            LOG.info("Retrieved {} filenames from metadata.", filesList.size());
            return filesList;
        } catch (SQLException e) {
            LOG.error("Error while trying to get the file names in the metadata table: {}", e.getMessage(), e);
            throw new RuntimeException("Error while trying to get the file names in the metadata table.", e);
        }
    }

    /**
     * Executes a query to check if the timetable files has already been readed and the name saved into metadata table.
     *
     * @param connection the database connection to use
     * @param fileName name of the timetable file
     * @return boolean tells if the file has already been readed and saved into the metadata table.
     */
    public boolean isFileNameInMetadata(Connection connection, String fileName) {
        String query = "SELECT distinct value FROM metadata WHERE value = ?";
        LOG.debug("Executing query: {}. With file name: {}", query, fileName);

        try (ResultSet resultSet = read(connection, query, fileName)) {
            String result = resultSet.next() ? resultSet.getString("value") : null;
            return result != null;
        } catch (SQLException e) {
            LOG.error("Error while trying to check if the file is in the metadata table: {}", e.getMessage(), e);
            throw new RuntimeException("Error while trying to check if the file is in the metadata table.", e);
        }
    }

    /**
     * Executes a query to check if the run should be started looking at the metada table and if there is already a run
     * with the same Run-Id.
     *
     * @param connection the database connection to use
     * @param runId given or generated Run-ID
     * @return boolean tells if the Run should be started.
     */
    public boolean checkForRunId(Connection connection, String runId) {
        String query = "SELECT DISTINCT run_id FROM metadata where run_id = ? ;";
        LOG.debug("Executing query: {}. With parameters: {}", query, runId);

        try (ResultSet resultSet = read(connection, query, runId)) {
            String result = resultSet.next() ? resultSet.getString("run_id") : null;
            if (result != null) {
                LOG.info("There is already a Run with the Run-ID: {}", runId);
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new RuntimeException("Error while trying to check if the file is in the metadata table.", e);
        }
    }

    /**
     * Executes a query to check if the run should be started looking at the metada table table if it already has a
     * combination of Timetable + Saison Base ID.
     *
     * @param connection the database connection to use
     * @param fileName name of the timetable file
     * @param saisonBaseId of the Saisons
     * @return boolean tells if the Run should be started.
     */
    public boolean shouldRunCron(Connection connection, String fileName, String saisonBaseId) {
        String query = "SELECT DISTINCT m1.RUN_ID "
                + "FROM metadata m1 "
                + "JOIN metadata m2 "
                + "  ON m1.RUN_ID = m2.RUN_ID "
                + "WHERE m1.KEY = '" + TIMETABLE + "'"
                + "  AND m2.KEY = '" + SAISON_BASE_ID + "'"
                + "  AND m1.VALUE = ? "
                + "  AND m2.VALUE = ? ;";
        LOG.debug("Executing query: {}. With parameters: {}, {}", query, fileName, saisonBaseId);

        try (ResultSet resultSet = read(connection, query, fileName, saisonBaseId)) {
            String result = resultSet.next() ? resultSet.getString("run_id") : null;
            if (result != null) {
                LOG.info("No Run needed as there is already a run with timetable {} and saison base id {}. Run: {}",
                        fileName, saisonBaseId, resultSet.getString("run_id"));
                return false;
            }
            return true;
        } catch (SQLException e) {
            throw new RuntimeException("Error while trying to check if the file is in the metadata table.", e);
        }
    }
}
