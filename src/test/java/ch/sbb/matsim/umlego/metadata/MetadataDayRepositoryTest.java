package ch.sbb.matsim.umlego.metadata;

import ch.sbb.matsim.umlego.util.RunId;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static ch.sbb.matsim.umlego.metadata.MetadataDayKey.*;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.SAISON_BASE_ID;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.TIMETABLE;
import static java.lang.String.valueOf;
import static org.junit.jupiter.api.Assertions.*;

public class MetadataDayRepositoryTest {

    private Connection connection;
    private MetadataDayRepository repository;
    private MetadataRepository runRepository;

    @BeforeEach
    public void setUp() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;NON_KEYWORDS=key,value");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        connection = dataSource.getConnection();

        // Create metadata_day table
        String createTableSQL = """
                CREATE TABLE metadata_day (
                    run_id VARCHAR(255),
                    target_date DATE,
                    key VARCHAR(255),
                    value VARCHAR(255)
                )
                """;
        connection.createStatement().execute(createTableSQL);
        createMetadataTable();

        repository = new MetadataDayRepository();
        runRepository = new MetadataRepository();
    }
    private void createMetadataTable() throws SQLException {
        String ddl = """
                CREATE TABLE metadata (
                    run_id VARCHAR(255),
                    key VARCHAR(255),
                    value VARCHAR(255)
                )
                """;
        connection.createStatement().execute(ddl);
    }

    @AfterEach
    public void tearDown() throws SQLException {
        connection.createStatement().execute("DROP TABLE metadata_day");
        connection.createStatement().execute("DROP TABLE metadata");
        connection.close();
    }

    @Test
    public void testInsertMetadata() throws SQLException {
        // Insert metadata
        repository.insertMetadata(connection, "run1", LocalDate.parse("2024-12-01"), RUNTIME, "value1");

        // Verify insertion
        String query = "SELECT * FROM metadata_day WHERE run_id = 'run1'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            var resultSet = stmt.executeQuery();
            resultSet.next();
            assertEquals("run1", resultSet.getString("run_id"));
            assertEquals(Date.valueOf("2024-12-01"), resultSet.getDate("target_date"));
            assertEquals("RUNTIME", resultSet.getString("key"));
            assertEquals("value1", resultSet.getString("value"));
        }
    }

    @Test
    public void testInsertMetadataNull() throws SQLException {
        // Insert metadata
        repository.insertMetadata(connection, "run1", LocalDate.parse("2024-12-01"), RUNTIME, null);

        // Verify insertion
        String query = "SELECT * FROM metadata_day WHERE run_id = 'run1'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            var resultSet = stmt.executeQuery();
            resultSet.next();
            assertEquals("run1", resultSet.getString("run_id"));
            assertEquals(Date.valueOf("2024-12-01"), resultSet.getDate("target_date"));
            assertEquals("RUNTIME", resultSet.getString("key"));
            assertNull(resultSet.getString("value"));
        }
    }

    @Test
    public void testInsertMultipleMetadata() throws SQLException {
        // Insert multiple records
        repository.insertMetadata(connection, "run1", LocalDate.parse("2024-11-01"), RUNTIME, "value1");
        repository.insertMetadata(connection, "run1", LocalDate.parse("2024-11-01"), BASE_DEMAND, "value2");

        // Verify insertion
        String query = "SELECT * FROM metadata_day WHERE run_id = 'run1'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            var resultSet = stmt.executeQuery();
            List<String> keys = new ArrayList<>();
            while (resultSet.next()) {
                keys.add(resultSet.getString("key"));
            }
            assertEquals(2, keys.size());
            assertEquals(List.of("RUNTIME", "BASE_DEMAND"), keys);
        }
    }

    @Test
    public void testNoRecordsForInvalidRunId() throws SQLException {
        // Verify no records exist for an invalid run_id
        String query = "SELECT * FROM metadata_day WHERE run_id = 'invalid_run'";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            var resultSet = stmt.executeQuery();
            assertFalse(resultSet.next());
        }
    }

    @Test
    public void testIsUmlegoValid() {
        repository.insertMetadata(connection, "run1", LocalDate.parse("2024-11-01"), VALID_AUTO, valueOf(true));
        boolean result = repository.isUmlegoValid(connection, new RunId("run1"), LocalDate.parse("2024-11-01"));
        assertTrue(result);
    }

    @Test
    void testFindByTargetDateSaisonBaseTimetable() {
        // Given
        String timetable = "2024-12-09_142022_001_SBB_Rohdaten_2025.zip";
        String saisonBaseId = "35_20241210";
        LocalDate targetDate = LocalDate.parse("2024-12-18");

        runRepository.insertMetadata(connection, "run1", SAISON_BASE_ID, saisonBaseId);
        runRepository.insertMetadata(connection, "run1", TIMETABLE, timetable);
        repository.insertMetadata(connection, "run1", targetDate, VALID_AUTO, valueOf(true));

        // When
        boolean result = repository.findByTargetDateSaisonBaseTimetable(connection, targetDate, saisonBaseId, timetable);

        // Then
        assertTrue(result);
    }

    @Test
    void testFindByTargetDateSaisonBaseTimetable_DayNotCalculated() {
        // Given
        String timetable = "2024-12-09_142022_001_SBB_Rohdaten_2025.zip";
        String saisonBaseId = "35_20241210";

        runRepository.insertMetadata(connection, "run1", SAISON_BASE_ID, saisonBaseId);
        runRepository.insertMetadata(connection, "run1", TIMETABLE, timetable);
        repository.insertMetadata(connection, "run1", LocalDate.parse("2024-12-18"), VALID_AUTO, valueOf(true));

        // When
        boolean result = repository.findByTargetDateSaisonBaseTimetable(connection, LocalDate.parse("2024-12-19"), saisonBaseId, timetable);

        // Then
        assertFalse(result);
    }

    @Test
    void testFindByTargetDateSaisonBaseTimetable_AnotherTimeTable() {
        // Given
        String timetable = "2024-12-09_142022_001_SBB_Rohdaten_2025.zip";
        String saisonBaseId = "35_20241210";
        LocalDate targetDate = LocalDate.parse("2024-12-18");

        runRepository.insertMetadata(connection, "run1", SAISON_BASE_ID, saisonBaseId);
        runRepository.insertMetadata(connection, "run1", TIMETABLE, timetable);
        repository.insertMetadata(connection, "run1", targetDate, VALID_AUTO, valueOf(true));

        // When
        boolean result = repository.findByTargetDateSaisonBaseTimetable(connection, targetDate, saisonBaseId, "another one");

        // Then
        assertFalse(result);
    }

    @Test
    void testFindByTargetDateSaisonBaseTimetable_AnotherSaisonBaseId() {
        // Given
        String timetable = "2024-12-09_142022_001_SBB_Rohdaten_2025.zip";
        String saisonBaseId = "35_20241210";
        LocalDate targetDate = LocalDate.parse("2024-12-18");

        runRepository.insertMetadata(connection, "run1", SAISON_BASE_ID, saisonBaseId);
        runRepository.insertMetadata(connection, "run1", TIMETABLE, timetable);
        repository.insertMetadata(connection, "run1", targetDate, VALID_AUTO, valueOf(true));

        // When
        boolean result = repository.findByTargetDateSaisonBaseTimetable(connection, targetDate, "another", timetable);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetInvalidTargetDates_WhenNoInvalidDatesExist() throws SQLException {
        // Given
        String runId = "test_run";
        LocalDate targetDate = LocalDate.of(2024, 1, 1);
        insertMetadataDay(runId, targetDate, "VALID_AUTO", "true");

        // When
        String result = repository.getInvalidTargetDates(connection, runId);

        // Then
        assertEquals("", result);
    }

    @Test
    void testGetInvalidTargetDates_WhenInvalidDatesExist() throws SQLException {
        // Given
        String runId = "test_run";
        LocalDate date1 = LocalDate.of(2024, 1, 1);
        LocalDate date2 = LocalDate.of(2024, 1, 2);
        insertMetadataDay(runId, date1, "VALID_AUTO", "false");
        insertMetadataDay(runId, date2, "VALID_AUTO", "false");

        // When
        String result = repository.getInvalidTargetDates(connection, runId);

        // Then
        assertEquals("2024-01-01, 2024-01-02", result);
    }

    @Test
    void testGetInvalidTargetDates_WhenNoRecordsExist() {
        // Given
        String runId = "nonexistent_run";

        // When
        String result = repository.getInvalidTargetDates(connection, runId);

        // Then
        assertEquals("", result);
    }

    private void insertMetadataDay(String runId, LocalDate targetDate, String key, String value) throws SQLException {
        String sql = "INSERT INTO metadata_day (run_id, target_date, key, value) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, runId);
            ps.setDate(2, Date.valueOf(targetDate));
            ps.setString(3, key);
            ps.setString(4, value);
            ps.executeUpdate();
        }
    }
}
