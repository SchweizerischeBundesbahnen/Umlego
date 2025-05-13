package ch.sbb.matsim.umlego.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SnowflakeUtil is a utility class for executing read-only SQL queries on a Snowflake database. It provides methods to
 * manage query execution with prepared statements, logging query details, and safely handling database resources.
 */
public final class SnowflakeUtil {

    private static final Logger LOG = LogManager.getLogger(SnowflakeUtil.class);

    private SnowflakeUtil() {
    }

    /**
     * Executes a read-only query on the Snowflake database using the provided connection and query parameters. Prepares
     * a statement, binds parameters, executes the query, and returns a ResultSet for processing.
     *
     * @param connection the active database connection
     * @param query the SQL query to execute
     * @param parameters the parameters for the prepared statement, bound in order of appearance in the query
     * @return a ResultSet containing the results of the query; must be closed by the caller after processing
     * @throws RuntimeException if an SQL error occurs during query execution or if the connection is null
     */
    public static ResultSet read(Connection connection, String query, Object... parameters) {
        PreparedStatement preparedStatement;
        ResultSet resultSet;

        try {
            if (connection != null) {
                preparedStatement = connection.prepareStatement(query);
                LOG.info("Preparing and executing query: {}", query);

                for (int i = 0; i < parameters.length; i++) {
                    preparedStatement.setObject(i + 1, parameters[i]);
                }

                String executedSQL = getExecutedSQL(query, parameters);
                LOG.info("Executing SQL with {} parameters: {}", parameters.length, executedSQL);

                resultSet = preparedStatement.executeQuery();
                return resultSet;
            } else {
                throw new RuntimeException("Failed to establish a connection.");
            }
        } catch (SQLException e) {
            LOG.error("Error executing query: {}", e.getMessage(), e);
            throw new RuntimeException("Error executing query on Snowflake", e);
        }
    }

    /**
     * Executes a write (INSERT, UPDATE, DELETE) query on the Snowflake database using the provided connection and query
     * parameters. Prepares a statement, binds parameters, executes the query, and returns the number of affected rows.
     * THis method is thought for single entries insertions and not a large amount of queries as prepareStatement()
     * method is an expensive operation.
     *
     * @param connection the active database connection
     * @param query the SQL query to execute
     * @param parameters the parameters for the prepared statement, bound in order of appearance in the query
     * @return the number of rows affected by the query
     * @throws RuntimeException if an SQL error occurs during query execution or if the connection is null
     */
    public static void write(Connection connection, String query, Object... parameters) {
        PreparedStatement preparedStatement;

        try {
            if (connection != null) {
                preparedStatement = connection.prepareStatement(query);
                LOG.info("Preparing and executing write query: {}", query);

                for (int i = 0; i < parameters.length; i++) {
                    preparedStatement.setObject(i + 1, parameters[i]);
                }

                String executedSQL = getExecutedSQL(query, parameters);
                LOG.info("Executing SQL with {} parameters: {}", parameters.length, executedSQL);

                int rowsAffected = preparedStatement.executeUpdate();
                LOG.info("Query executed successfully, {} rows affected.", rowsAffected);
            } else {
                throw new RuntimeException("Failed to establish a connection.");
            }
        } catch (SQLException e) {
            LOG.error("Error executing write query: {}", e.getMessage(), e);
            throw new RuntimeException("Error executing write query on Snowflake", e);
        }
    }

    /**
     * Formats the SQL query with the provided parameters for logging purposes. Replaces each "?" in the query with the
     * corresponding parameter from the array.
     *
     * @param sql the SQL query with placeholders
     * @param parameters the parameters to replace placeholders in the query
     * @return a String representing the SQL query with parameters in place of placeholders
     */
    private static String getExecutedSQL(String sql, Object[] parameters) {
        if (parameters == null || parameters.length == 0) {
            return sql;
        }
        StringBuilder executedSQL = new StringBuilder(sql);
        int index;
        for (Object param : parameters) {
            index = executedSQL.indexOf("?");
            if (index == -1) {
                break;
            }
            executedSQL.replace(index, index + 1, param == null ? "null" : param.toString());
        }
        return executedSQL.toString();
    }
}
