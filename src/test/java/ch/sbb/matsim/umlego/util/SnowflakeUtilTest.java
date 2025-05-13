package ch.sbb.matsim.umlego.util;

import static ch.sbb.matsim.umlego.util.SnowflakeUtil.read;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SnowflakeUtilTest {

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
        String dropTableSQL = "DROP TABLE IF EXISTS dummy_table";
        connection.createStatement().execute(dropTableSQL);

        // Create a dummy table
        String createTableSQL = """
            CREATE TABLE dummy_table (
                id INT PRIMARY KEY,
                name VARCHAR(50),
                age INT
            )
            """;
        connection.createStatement().execute(createTableSQL);

        // Insert test data
        insertTestData(1, "Alice", 30);
        insertTestData(2, "Bob", 40);
    }

    private void insertTestData(int id, String name, int age) throws SQLException {
        String insertSQL = "INSERT INTO dummy_table (id, name, age) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
            statement.setInt(1, id);
            statement.setString(2, name);
            statement.setInt(3, age);
            statement.executeUpdate();
        }
    }

    @AfterEach
    public void tearDown() throws SQLException {
        connection.close();
    }

    @Test
    public void testRead_withValidQuery() throws SQLException {
        // Execute read query
        String query = "SELECT * FROM dummy_table WHERE age > ?";
        try (ResultSet resultSet = read(connection, query, 35)) {
            assertNotNull(resultSet);

            // Verify the results
            resultSet.next();
            assertEquals(2, resultSet.getInt("id"));
            assertEquals("Bob", resultSet.getString("name"));
            assertEquals(40, resultSet.getInt("age"));
        }
    }

    @Test
    public void testRead_withNoMatchingData() throws SQLException {
        // Execute read query with no matching data
        String query = "SELECT * FROM dummy_table WHERE age > ?";
        try (ResultSet resultSet = read(connection, query, 50)) {
            assertNotNull(resultSet);
            // Verify that no results are returned
            assertFalse(resultSet.next());
        }
    }
}
