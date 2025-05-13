package ch.sbb.matsim.umlego.config.snowflake;

import ch.sbb.matsim.umlego.config.EnvironmentUtil;
import ch.sbb.matsim.umlego.config.credentials.SnowflakeCredentials;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static ch.sbb.matsim.umlego.config.EnvironmentUtil.getStage;
import static ch.sbb.matsim.umlego.config.credentials.SnowflakeCredentials.getPassword;
import static ch.sbb.matsim.umlego.config.credentials.SnowflakeCredentials.getUserName;


/**
 * SnowflakeConfig is a utility class for connecting to a Snowflake database.
 */
public final class SnowflakeConfig {

    private static final Logger LOG = LogManager.getLogger(SnowflakeConfig.class);

    private static final String USER = getUserName();
    private static final String PASSWORD = getPassword();
    private static final String WAREHOUSE = getWarehouse();
    private static final String DATABASE = getDatabase();
    private static final String SCHEMA = "internal_tagesprognose";
    private static final String URL = "jdbc:snowflake://sbb-eap.snowflakecomputing.com";

    private SnowflakeConfig() {}

    /**
     * Establishes a connection to the Snowflake database using the provided credentials and settings.
     *
     * @return a Connection object for interacting with the Snowflake database
     * @throws RuntimeException if an error occurs during connection
     */
    public static Connection connect() {
        try {
            Properties props = new Properties();
            props.put("user", USER);
            props.put("password", PASSWORD);
            props.put("warehouse", WAREHOUSE);
            props.put("db", DATABASE);
            props.put("schema", SCHEMA);
            props.put("TIMEZONE", "UTC");
            props.put("CLIENT_RESULT_COLUMN_CASE_INSENSITIVE", "true");
            props.put("CLIENT_TIMESTAMP_TYPE_MAPPING", "TIMESTAMP_NTZ");
            props.put("CLIENT_SESSION_KEEP_ALIVE", "true");
            props.put("JDBC_QUERY_RESULT_FORMAT", "JSON");

            LOG.info("Attempting to connect to Snowflake...");
            Class.forName("net.snowflake.client.jdbc.SnowflakeDriver");
            Connection connection = DriverManager.getConnection(URL, props);
            LOG.info("Connected to Snowflake successfully.");
            return connection;
        } catch (SQLException e) {
            throw new RuntimeException("Error connecting to Snowflake", e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Snowflake JDBC Driver not found", e);
        }
    }

    /**
     * Closes connection to the Snowflake database.
     *
     * @throws RuntimeException if an error occurs during connection closing
     */
    public static void closeConnection(Connection connection) {
        try {
            connection.close();
            LOG.info("Connection closed successfully.");
        } catch (SQLException e) {
            throw new RuntimeException("Error closing connection: ", e);
        }
    }

    private static String getWarehouse() {
        return String.format("%s_simba_load_wh", getStage());
    }

    private static String getDatabase() {
        return String.format("%s_std_simba_db", getStage());
    }
}
