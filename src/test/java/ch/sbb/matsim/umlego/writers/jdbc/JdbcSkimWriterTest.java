package ch.sbb.matsim.umlego.writers.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;
import ch.sbb.matsim.umlego.writers.jdbc.SkimRepository.SkimEntry;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JdbcSkimWriterTest {

    private JdbcSkimWriter writer;
    private final JdbcDataSource dataSource = new JdbcDataSource();

    @BeforeEach
    public void setUp() throws SQLException {
        // Set up in-memory database
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        Connection connection = dataSource.getConnection();

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
        // Create the table
        connection.createStatement().execute(createTableSQL);

        // Initialize writer
        writer = new JdbcSkimWriter(connection, "run1", LocalDate.parse("2024-12-24"));
    }

    @AfterEach
    public void tearDown() throws SQLException {
        Connection connectionTearDown = dataSource.getConnection();
        connectionTearDown.createStatement().execute("DROP TABLE skim");
        connectionTearDown.close();
    }

    @Test
    public void testWriteRouteAndClose() throws Exception {
        FoundRoute foundRoute = mock(FoundRoute.class);
        Object2DoubleMap<String> demandMap = new Object2DoubleOpenHashMap<>();
        demandMap.put("2", 100.0);
        foundRoute.demand = demandMap;
        foundRoute.arrTime = 7200.0;
        foundRoute.depTime = 3600.0;
        foundRoute.transfers = 2;

        Object2DoubleMap<String> adaptationTimeMap = new Object2DoubleOpenHashMap<>();
        adaptationTimeMap.put("2", 300.0);
        foundRoute.adaptationTime = adaptationTimeMap;

        writer.writeRoute("1", "2", foundRoute);
        writer.close();

        Connection connectionTest = dataSource.getConnection();

        // Verify data in the database
        String query = "SELECT * FROM skim";
        try (Statement stmt = connectionTest.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            assertTrue(rs.next());

            SkimEntry entry = new SkimEntry(
                    rs.getString("RUN_ID"),
                    rs.getDate("TARGET_DATE").toLocalDate(),
                    rs.getInt("ORIGIN"),
                    rs.getInt("DESTINATION"),
                    rs.getDouble("DEMAND"),
                    rs.getInt("ROUTE_COUNT"),
                    rs.getInt("TOTAL_JOURNEY_TIME"),
                    rs.getDouble("JOURNEY_TIME_WEIGHTED"),
                    rs.getDouble("TRANSFER_COUNT_WEIGHTED"),
                    rs.getDouble("ADAPTATION_TIME_WEIGHTED")
            );

            // Verify the values
            assertEquals("run1", entry.runId());
            assertEquals(1, entry.origin());
            assertEquals(2, entry.destination());
            assertEquals(100.0, entry.demand());
            assertEquals(1, entry.routeCount());
            assertEquals(3600, entry.totalJourneyTime());
            assertEquals(360000.0, entry.journeyTimeWeighted()); // 100 * (7200 - 3600)
            assertEquals(200.0, entry.transferCountWeighted()); // 100 * 2
            assertEquals(30000.0, entry.adaptationTimeWeighted()); // 100 * 300.0

            assertFalse(rs.next());
        }
        connectionTest.close();
    }
}
