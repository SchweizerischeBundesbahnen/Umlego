package ch.sbb.matsim.umlego.metadata;

import static ch.sbb.matsim.umlego.metadata.MetadataKey.RUNTIME;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.SAISON_BASE_ID;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.TIMETABLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MetadataRepositoryTest {

    private Connection connection;
    private MetadataRepository repository;

    @BeforeEach
    public void setUp() throws SQLException {
        // Set up H2 in-memory database
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;NON_KEYWORDS=key,value");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        connection = dataSource.getConnection();

        // Create metadata table
        String createTableSQL = """
                CREATE TABLE metadata (
                    run_id VARCHAR(255),
                    key VARCHAR(255),
                    value VARCHAR(255)
                )
                """;
        connection.createStatement().execute(createTableSQL);

        repository = new MetadataRepository();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        connection.createStatement().execute("DROP TABLE metadata");
        connection.close();
    }

    @Test
    public void testInsertMetadata() throws SQLException {
        // Insert metadata
        repository.insertMetadata(connection, "run1", TIMETABLE, "file1.csv");

        // Verify insertion
        String query = "SELECT * FROM metadata WHERE run_id = 'run1'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            var resultSet = stmt.executeQuery();
            resultSet.next();
            assertEquals("run1", resultSet.getString("run_id"));
            assertEquals("TIMETABLE", resultSet.getString("key"));
            assertEquals("file1.csv", resultSet.getString("value"));
        }
    }

    @Test
    public void testFindAllProcessedTimetables_WithMatchingFiles_And_OtherMetadataEntrie() throws SQLException {
        // Insert test data
        insertTestData("run1", TIMETABLE, "file1.csv");
        insertTestData("run1", TIMETABLE, "file2.csv");
        insertTestData("run2", RUNTIME, "test");

        // Query files
        List<String> files = repository.findAllProcessedTimetables(connection);

        // Verify results
        assertEquals(2, files.size());
        assertEquals(Arrays.asList("file1.csv", "file2.csv"), files);
    }

    @Test
    public void testFindAllProcessedTimetables_WithNoMatchingFiles() throws SQLException {
        // Insert test data
        insertTestData("run1", TIMETABLE, "file1.csv");

        // Query files
        List<String> files = repository.findAllProcessedTimetables(connection);

        // Verify empty result
        assertEquals(1, files.size());
    }

    @Test
    public void testFindAllProcessedTimetables_WithEmptyTable() {
        List<String> files = repository.findAllProcessedTimetables(connection);

        assertEquals(0, files.size());
    }

    @Test
    public void testIsFileName_FileExistsInMetadata() throws SQLException {
        // Insert test data
        insertTestData("run1", TIMETABLE, "file1.csv");

        // Verify file exists
        boolean exists = repository.isFileNameInMetadata(connection, "file1.csv");
        assertTrue(exists);
    }

    @Test
    public void testIsFileName_FileDoesNotExistInMetadata() throws SQLException {
        // Insert test data
        insertTestData("run1", TIMETABLE, "file1.csv");

        // Verify file does not exist
        boolean exists = repository.isFileNameInMetadata(connection, "file2.csv");
        assertFalse(exists);
    }

    @Test
    public void testIsFileName_InMetadata_WithEmptyTable() {
        // Verify file does not exist in empty table
        boolean exists = repository.isFileNameInMetadata(connection, "file1.csv");
        assertFalse(exists);
    }

    @Test
    public void testCheckForRunId_RunExists() throws SQLException {
        // Insert test data
        insertTestData("run1", TIMETABLE, "timetable1.zip");

        // Verify the run exists
        boolean exists = repository.checkForRunId(connection, "run1");
        assertTrue(exists);
    }

    @Test
    public void testCheckForRunId_RunDoesNotExist() throws SQLException {
        // Insert test data
        insertTestData("run1", TIMETABLE, "timetable1.zip");

        // Verify the run does not exist
        boolean exists = repository.checkForRunId(connection, "run2");
        assertFalse(exists);
    }

    @Test
    public void testCheckForRunId_EmptyTable() {
        // Verify the run does not exist in an empty table
        boolean exists = repository.checkForRunId(connection, "run1");
        assertFalse(exists);
    }


    @Test
    public void testShouldRunCron_RunNotNeeded() throws SQLException {
        // Insert test data
        insertTestData("run1", TIMETABLE, "timetable1.zip");
        insertTestData("run1", SAISON_BASE_ID, "34_123456");

        // Verify that the run should not be started
        boolean shouldRun = repository.shouldRunCron(connection, "timetable1.zip", "34_123456");
        assertFalse(shouldRun);
    }

    @Test
    public void testShouldRunCron_RunNeeded_TimetableMissing() throws SQLException {
        // Insert test data
        insertTestData("run1", SAISON_BASE_ID, "34_123456");

        // Verify that the run should be started
        boolean shouldRun = repository.shouldRunCron(connection, "timetable1.zip", "34_123456");
        assertTrue(shouldRun);
    }

    @Test
    public void testShouldRunCron_RunNeeded_SaisonBaseIdMissing() throws SQLException {
        // Insert test data
        insertTestData("run1", TIMETABLE, "timetable1.zip");

        // Verify that the run should be started
        boolean shouldRun = repository.shouldRunCron(connection, "timetable1.zip", "34_123456");
        assertTrue(shouldRun);
    }

    @Test
    public void testShouldRunCron_RunNeeded_EmptyTable() {
        // Verify that the run should be started with an empty table
        boolean shouldRun = repository.shouldRunCron(connection, "timetable1.zip", "34_123456");
        assertTrue(shouldRun);
    }

    @Test
    public void testShouldRunCron_RunNeeded_DifferentRunId() throws SQLException {
        // Insert test data with different RUN_IDs
        insertTestData("run1", TIMETABLE, "timetable1.zip");
        insertTestData("run2", SAISON_BASE_ID, "34_123456");

        // Verify that the run should be started
        boolean shouldRun = repository.shouldRunCron(connection, "timetable1.zip", "34_123456");
        assertTrue(shouldRun);
    }

    private void insertTestData(String runId, MetadataKey key, String value) throws SQLException {
        String insertSQL = "INSERT INTO metadata (run_id, key, value) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSQL)) {
            statement.setString(1, runId);
            statement.setString(2, key.toString());
            statement.setString(3, value);
            statement.executeUpdate();
        }
    }
}
