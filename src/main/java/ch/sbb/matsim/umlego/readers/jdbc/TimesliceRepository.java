package ch.sbb.matsim.umlego.readers.jdbc;

import ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig;
import ch.sbb.matsim.umlego.matrix.MatrixUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static ch.sbb.matsim.umlego.util.SnowflakeUtil.read;

/**
 * TimesliceRepository provides methods for reading and parsing matrix data from a database.
 * It is designed to retrieve specific entries from the Simba database based on simbaRunId and season type, and transform
 * the result set into a list of TimesliceJdbcEntry records.
 */
public final class TimesliceRepository {

    static Logger LOG = LogManager.getLogger(TimesliceRepository.class);

    public TimesliceRepository() {
    }

    /**
     * TimesliceJdbcEntry is a record that represents a single entry in the matrix, containing matrix index, origin, destination, and a numerical value.
     *
     * @param matrixIndex the index of the matrix
     * @param from the source district
     * @param to the destination district
     * @param value the number of travelers or value associated with this entry
     */
    public record TimesliceJdbcEntry(Integer matrixIndex, String from, String to, double value) {
    }

    /**
     * Executes a query to read matrix data from the simba_tgm_zeitscheibe table, filtering results by run ID and season type.
     *
     * @param connection the database connection to use
     * @param simbaRunId the Simba-Run identifier for filtering
     * @param saison the season type to filter results
     * @return ResultSet containing the matrix data based on the provided run ID and season type
     */
    public List<TimesliceJdbcEntry> readMatrices(Connection connection, String simbaRunId, String saison) {
        String query = "SELECT quell_bezirk_nummer, ziel_bezirk_nummer, reisende, scheibe FROM simba.simba_tgm_zeitscheibe WHERE run_id = ? AND tag_typ = ? AND reisende != 0";
        ResultSet resultSet = read(connection, query, simbaRunId, saison);
        return parseMatrix(resultSet);
    }

    /**
     * Parses the matrix data based on the given season and database connection.
     *
     * @param resultSetMatrices ResultSet with the readed Matrices
     * @return a list of TimesliceJdbcEntry records representing matrix data
     * @throws SQLException if an SQL error occurs during data retrieval
     */
    public static List<TimesliceJdbcEntry> parseMatrix(ResultSet resultSetMatrices) {
        List<TimesliceJdbcEntry> timesliceJdbcEntries = new ArrayList<>();

        try {
            while (resultSetMatrices.next()) {
                int matrixMinute = resultSetMatrices.getInt("scheibe");
                int matrixIndex = MatrixUtil.minutesToMatrixIndex(matrixMinute);
                String from = resultSetMatrices.getString("quell_bezirk_nummer");
                String to = resultSetMatrices.getString("ziel_bezirk_nummer");
                double value = resultSetMatrices.getDouble("reisende");

                timesliceJdbcEntries.add(new TimesliceJdbcEntry(matrixIndex, from, to, value));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error parsing matrix data from ResultSet", e);
        } finally {
            try {
                if (resultSetMatrices != null) {
                    resultSetMatrices.close();
                }
            } catch (SQLException e) {
               LOG.error("Error closing ResultSet: " + e.getMessage());
            }
        }
        LOG.info(timesliceJdbcEntries.size() + " matrix entries found");
        return timesliceJdbcEntries;
    }

    /**
     * Checks whether the saison base id is on snowflake or not.
     *
     * @param connection A database connection
     * @param saisonBaseId The saison base id
     * @return true if the saison base id is in the database
     * @throws SQLException
     */
    public static boolean isSaisonBaseIdOnSnowflake(Connection connection, String saisonBaseId) throws SQLException {
        LOG.info("Checking saisonBaseId : {}", saisonBaseId);
        String query = "SELECT EXISTS (SELECT 1 FROM simba.simba_tgm_zeitscheibe WHERE run_id LIKE '" + saisonBaseId + "_%')";
        ResultSet resultSet = read(connection, query);
        resultSet.next();
        boolean output = resultSet.getBoolean(1);
        resultSet.close();
        LOG.info("isSaisonBaseIdOnSnowflake result : {}", output);
        return output;
    }

    /**
     * Checks whether the saison run id is on snowflake or not.
     *
     * @param connection A database connection
     * @param saisonRunId The saison run id
     * @return true if the saison run id is in the database
     * @throws SQLException
     */
    public static boolean isSaisonRunIdOnSnowflake(Connection connection, String saisonRunId) throws SQLException {
        LOG.info("Checking saisonRunId : {}", saisonRunId);
        String query = "SELECT EXISTS (SELECT 1 FROM simba.simba_tgm_zeitscheibe WHERE run_id = ?)";
        ResultSet resultSet = read(connection, query, saisonRunId);
        resultSet.next();
        boolean output = resultSet.getBoolean(1);
        resultSet.close();
        LOG.info("isSaisonRunIdOnSnowflake result : {}", output);
        return output;
    }
}
