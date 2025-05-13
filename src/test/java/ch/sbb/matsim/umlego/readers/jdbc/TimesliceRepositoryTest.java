package ch.sbb.matsim.umlego.readers.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TimesliceRepositoryTest {

    private Connection connection;

    @BeforeEach
    public void setUp() throws SQLException {
        // Set up H2 in-memory database
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        connection = dataSource.getConnection();

        // Drop the table if it already exists
        String dropTableSQL = "DROP TABLE IF EXISTS simba_tgm_zeitscheibe";
        connection.createStatement().execute(dropTableSQL);

        // Create test table structure
        String createTableSQL = """
            CREATE TABLE simba.simba_tgm_zeitscheibe (
                scheibe INT,
                quell_bezirk_nummer VARCHAR(50),
                ziel_bezirk_nummer VARCHAR(50),
                reisende DOUBLE,
                run_id VARCHAR(50),
                tag_typ VARCHAR(50)
            )
            """;

        connection.createStatement().execute("CREATE SCHEMA IF NOT EXISTS simba");
        connection.createStatement().execute(createTableSQL);

        // Insert test data
        insertTestData(10, "101", "202", 50.5, "34_test", "SO_w");
        insertTestData(20, "103", "204", 75.0, "34_test", "SO_w");
    }

    private void insertTestData(int matrixMinute, String from, String to, double value, String runId, String season) throws SQLException {
        String insertSQL = "INSERT INTO simba.simba_tgm_zeitscheibe (scheibe, quell_bezirk_nummer, ziel_bezirk_nummer, reisende, run_id, tag_typ) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
            statement.setInt(1, matrixMinute);
            statement.setString(2, from);
            statement.setString(3, to);
            statement.setDouble(4, value);
            statement.setString(5, runId);
            statement.setString(6, season);
            statement.executeUpdate();
        }
    }

    @AfterEach
    public void tearDown() throws SQLException {
        connection.createStatement().execute("DROP TABLE simba.simba_tgm_zeitscheibe");
        connection.close();
    }

    @Test
    public void testReadMatrices_withValidData() {
        // Test reading matrix data
        TimesliceRepository timesliceRepository = new TimesliceRepository();
        List<TimesliceRepository.TimesliceJdbcEntry> result = timesliceRepository.readMatrices(connection, "34_test", "SO_w");

        // Verify the results
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).matrixIndex());
        assertEquals("101", result.get(0).from());
        assertEquals("202", result.get(0).to());
        assertEquals(50.5, result.get(0).value(), 0.01);

        assertEquals(2, result.get(1).matrixIndex());
        assertEquals("103", result.get(1).from());
        assertEquals("204", result.get(1).to());
        assertEquals(75.0, result.get(1).value(), 0.01);
    }

    @Test
    public void testReadMatrices_withNoMatchingData() throws SQLException {
        // Test with non-matching run ID and season
        TimesliceRepository timesliceRepository = new TimesliceRepository();
        List<TimesliceRepository.TimesliceJdbcEntry> result = timesliceRepository.readMatrices(connection, "no_existent_run_id", "no_existent_saison");

        // Verify that no results are returned
        assertEquals(0, result.size());
    }

    @Test
    public void testIsSaisonBaseIdOnSnowflake() throws SQLException {
        assertTrue(TimesliceRepository.isSaisonBaseIdOnSnowflake(connection, "34"));
        assertFalse(TimesliceRepository.isSaisonBaseIdOnSnowflake(connection, "36"));
    }

    @Test
    public void testIsSaisonRunIdOnSnowflake() throws SQLException {
        assertTrue(TimesliceRepository.isSaisonRunIdOnSnowflake(connection, "34_test"));
        assertFalse(TimesliceRepository.isSaisonRunIdOnSnowflake(connection, "34"));
        assertFalse(TimesliceRepository.isSaisonRunIdOnSnowflake(connection, "34_nope"));
        assertFalse(TimesliceRepository.isSaisonRunIdOnSnowflake(connection, "36"));
    }
}
