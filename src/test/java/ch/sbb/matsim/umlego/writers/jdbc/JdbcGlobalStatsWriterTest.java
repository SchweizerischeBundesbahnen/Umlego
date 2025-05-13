package ch.sbb.matsim.umlego.writers.jdbc;

import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.closeConnection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.sbb.matsim.umlego.metadata.MetadataDayKey;
import ch.sbb.matsim.umlego.writers.AbstractGlobalStatsWriter.StatisticsAccumulator;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JdbcGlobalStatsWriterTest {

    private JdbcGlobalStatsWriter writer;
    private LocalDate targetDate;
    private final JdbcDataSource dataSource = new JdbcDataSource();

    @BeforeEach
    public void setUp() throws Exception {
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;NON_KEYWORDS=key,value");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        Connection connection = dataSource.getConnection();

        String createTableSQL = """
                CREATE TABLE metadata_day (
                    RUN_ID VARCHAR(255),
                    TARGET_DATE DATE,
                    KEY VARCHAR(255),
                    VALUE DOUBLE
                )
                """;
        connection.createStatement().execute(createTableSQL);

        targetDate = LocalDate.of(2024, 12, 12);
        writer = new JdbcGlobalStatsWriter(connection, "testRun", targetDate);
    }

    @AfterEach
    public void tearDown() throws Exception {
        Connection connectionTearDown = dataSource.getConnection();
        connectionTearDown.createStatement().execute("DROP TABLE metadata_day");
        connectionTearDown.close();
    }

    @Test
    public void testCreateStatsMap() {
        Map<String, StatisticsAccumulator> statsMap = new HashMap<>();
        StatisticsAccumulator stats1 = new StatisticsAccumulator();
        stats1.totalPersonKm = 1000.0;
        stats1.totalPersonHours = 50.0;
        statsMap.put("RAIL", stats1);

        StatisticsAccumulator stats2 = new StatisticsAccumulator();
        stats2.totalPersonKm = 2000.0;
        stats2.totalPersonHours = 100.0;
        statsMap.put("EV", stats2);

        writer.totalDemand = 100.0;
        writer.totalWeightedAdaptationTime = 500.0;
        writer.totalWeightedTransfers = 300.0;

        writer.createStatsMap(statsMap);
        Map<String, Double> statsMapResult = writer.getGlobalStats();

        assertEquals(7, statsMapResult.size());
        assertEquals(1000.0, statsMapResult.get(MetadataDayKey.PERSON_KM + "_RAIL"));
        assertEquals(50.0, statsMapResult.get(MetadataDayKey.PERSON_HOURS + "_RAIL"));
        assertEquals(2000.0, statsMapResult.get(MetadataDayKey.PERSON_KM + "_EV"));
        assertEquals(100.0, statsMapResult.get(MetadataDayKey.PERSON_HOURS + "_EV"));
        assertEquals(5.0, statsMapResult.get(MetadataDayKey.ADAPTATION_TIME_WEIGHTED.toString())); // 500 / 100
        assertEquals(3.0, statsMapResult.get(MetadataDayKey.TRANSFERS_WEIGHTED.toString())); // 300 / 100
        assertEquals(100.0, statsMapResult.get(MetadataDayKey.ROUTED_DEMAND.toString()));
    }

    @Test
    public void testClose() throws Exception {
        writer.statsMap = new HashMap<>();
        StatisticsAccumulator stats = new StatisticsAccumulator();
        stats.totalPersonKm = 1500.0;
        stats.totalPersonHours = 75.0;
        writer.statsMap.put("EV", stats);
        writer.totalDemand = 100.0;
        writer.totalWeightedAdaptationTime = 400.0;
        writer.totalWeightedTransfers = 200.0;
        writer.close();

        Connection connectionTest = dataSource.getConnection();

        String query = "SELECT * FROM metadata_day ORDER BY key";
        try (Statement stmt = connectionTest.createStatement(); ResultSet rs = stmt.executeQuery(query)) {

            assertTrue(rs.next());
            assertEquals("testRun", rs.getString("RUN_ID"));
            assertEquals(String.valueOf(targetDate), String.valueOf(rs.getDate("TARGET_DATE")));
            assertEquals(MetadataDayKey.ADAPTATION_TIME_WEIGHTED.toString(), rs.getString("KEY"));
            assertEquals(4.0, rs.getDouble("VALUE")); // 400 / 100

            assertTrue(rs.next());
            assertEquals(MetadataDayKey.PERSON_HOURS + "_EV", rs.getString("KEY"));
            assertEquals(75.0, rs.getDouble("VALUE"));

            assertTrue(rs.next());
            assertEquals(MetadataDayKey.PERSON_KM + "_EV", rs.getString("KEY"));
            assertEquals(1500.0, rs.getDouble("VALUE"));

            assertTrue(rs.next());
            assertEquals(MetadataDayKey.ROUTED_DEMAND.toString(), rs.getString("KEY"));
            assertEquals(100.0, rs.getDouble("VALUE"));

            assertTrue(rs.next());
            assertEquals(MetadataDayKey.TRANSFERS_WEIGHTED.toString(), rs.getString("KEY"));
            assertEquals(2.0, rs.getDouble("VALUE")); // 200 / 100
        }

        closeConnection(connectionTest);
    }
}
