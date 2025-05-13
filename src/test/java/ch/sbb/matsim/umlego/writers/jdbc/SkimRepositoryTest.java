package ch.sbb.matsim.umlego.writers.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.sbb.matsim.umlego.writers.jdbc.SkimRepository.SkimEntry;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SkimRepositoryTest {

    private Connection connection;
    private SkimRepository repository;

    @BeforeEach
    public void setUp() throws SQLException {
        // Set up H2 in-memory database
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        connection = dataSource.getConnection();

        // Create simba_umlego_skims table
        String createTableSQL = """
                CREATE TABLE skim (
                    RUN_ID VARCHAR(255),
                    TARGET_DATE DATE,
                    ORIGIN INTEGER,
                    DESTINATION INTEGER,
                    DEMAND DOUBLE,
                    ROUTE_COUNT INTEGER,
                    TOTAL_JOURNEY_TIME INTEGER,
                    JOURNEY_TIME_WEIGHTED DOUBLE,
                    TRANSFER_COUNT_WEIGHTED DOUBLE,
                    ADAPTATION_TIME_WEIGHTED DOUBLE
                )
                """;
        connection.createStatement().execute(createTableSQL);

        repository = new SkimRepository();
    }

    @AfterEach
    public void tearDown() throws SQLException {
        connection.createStatement().execute("DROP TABLE skim");
        connection.close();
    }

    @Test
    public void testInsertEntries() throws SQLException {
        // Prepare test data
        List<SkimEntry> entries = new ArrayList<>();
        entries.add(new SkimEntry(
                "run1",
                LocalDate.parse("2024-12-24"),
                100,
                200,
                50.0,
                3,
                200,
                150.0,
                2.5,
                100.0
        ));
        entries.add(new SkimEntry(
                "run1",
                LocalDate.parse("2024-12-24"),
                101,
                201,
                75.0,
                4,
                400,
                300.0,
                3.0,
                200.0
        ));

        // Call the method under test
        repository.insertEntries(connection, entries);

        // Verify that data was inserted
        String query = "SELECT * FROM skim";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            List<SkimEntry> results = new ArrayList<>();
            while (rs.next()) {
                SkimEntry entry = new SkimEntry(
                        rs.getString("RUN_ID"),
                        LocalDate.parse("2024-12-24"),
                        rs.getInt("ORIGIN"),
                        rs.getInt("DESTINATION"),
                        rs.getDouble("DEMAND"),
                        rs.getInt("ROUTE_COUNT"),
                        rs.getInt("TOTAL_JOURNEY_TIME"),
                        rs.getDouble("JOURNEY_TIME_WEIGHTED"),
                        rs.getDouble("TRANSFER_COUNT_WEIGHTED"),
                        rs.getDouble("ADAPTATION_TIME_WEIGHTED")
                );
                results.add(entry);
            }

            assertEquals(2, results.size());

            // Verify first entry
            SkimEntry firstEntry = results.get(0);
            assertEquals("run1", firstEntry.runId());
            assertEquals(100, firstEntry.origin());
            assertEquals(200, firstEntry.destination());
            assertEquals(50.0, firstEntry.demand());
            assertEquals(3, firstEntry.routeCount());
            assertEquals(200, firstEntry.totalJourneyTime());
            assertEquals(150.0, firstEntry.journeyTimeWeighted());
            assertEquals(2.5, firstEntry.transferCountWeighted());
            assertEquals(100.0, firstEntry.adaptationTimeWeighted());

            // Verify second entry
            SkimEntry secondEntry = results.get(1);
            assertEquals("run1", secondEntry.runId());
            assertEquals(101, secondEntry.origin());
            assertEquals(201, secondEntry.destination());
            assertEquals(75.0, secondEntry.demand());
            assertEquals(4, secondEntry.routeCount());
            assertEquals(400, secondEntry.totalJourneyTime());
            assertEquals(300.0, secondEntry.journeyTimeWeighted());
            assertEquals(3.0, secondEntry.transferCountWeighted());
            assertEquals(200.0, secondEntry.adaptationTimeWeighted());
        }

    }

    @Test
    public void testInsertEntries_EmptyList() throws SQLException {
        // Prepare empty test data
        List<SkimEntry> entries = new ArrayList<>();

        // Call the method under test
        repository.insertEntries(connection, entries);

        // Verify that no data was inserted
        String query = "SELECT COUNT(*) AS rowcount FROM skim";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            rs.next();
            int count = rs.getInt("rowcount");
            assertEquals(0, count);
        }
    }
}
