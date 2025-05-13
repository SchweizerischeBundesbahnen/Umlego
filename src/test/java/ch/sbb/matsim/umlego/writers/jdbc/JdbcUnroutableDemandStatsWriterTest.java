/*
package ch.sbb.matsim.umlego.writers.jdbc;

import static ch.sbb.matsim.umlego.metadata.MetadataDayKey.*;
import static org.assertj.core.api.Assertions.assertThat;

import ch.sbb.matsim.umlego.demand.UnroutableDemand;
import ch.sbb.matsim.umlego.demand.UnroutableDemandPart;
import ch.sbb.matsim.umlego.demand.UnroutableDemandStats;
import ch.sbb.matsim.umlego.writers.types.skim.Skim;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.Map;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcUnroutableDemandStatsWriterTest {

    private JdbcUnroutableDemandStatsWriter writer;
    private UnroutableDemandStats stats;
    private final JdbcDataSource dataSource = new JdbcDataSource();
    private Connection connection;
    private final LocalDate targetDate = LocalDate.parse("2024-12-24");

    @BeforeEach
    void setUp() throws Exception {
        // Set up in-memory database
        dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;NON_KEYWORDS=key,value");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        connection = dataSource.getConnection();

        String createTableSQL = """
                CREATE TABLE metadata_day (
                    RUN_ID      VARCHAR(255),
                    TARGET_DATE DATE,
                    KEY         VARCHAR(255),
                    VALUE       VARCHAR(255)
                );
                """;
        connection.createStatement().execute(createTableSQL);

        // Initialize writer
        writer = new JdbcUnroutableDemandStatsWriter(connection, "run1", targetDate);

        // Test data with high unroutable demand to exceed 0.95 limit
        UnroutableDemand unroutableDemand = new UnroutableDemand();
        unroutableDemand.addPart(new UnroutableDemandPart("zone1", "zone2", 950.0));
        unroutableDemand.addPart(new UnroutableDemandPart("zone1", "zone3", 50.0));

        // Only 1.0 routable demand to ensure share is > 0.95
        Map<String, Double> statsMap = Map.of(ROUTED_DEMAND.toString(), 1.0);
        Skim skims = new Skim();

        stats = new UnroutableDemandStats(unroutableDemand, statsMap, skims);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.createStatement().execute("DROP TABLE metadata_day");
        connection.close();
    }

    private String getMetadataValue(String key) throws Exception {
        String query = "SELECT value FROM metadata_day WHERE key = ? AND run_id = ? AND target_date = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, key);
            stmt.setString(2, "run1");
            stmt.setDate(3, java.sql.Date.valueOf(targetDate));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getString("value") : null;
            }
        }
    }

    @Test
    void testWriteUnroutableDemandStats() throws Exception {
        // Write stats
        writer.write(stats);

        // Verify each metadata value individually
        assertThat(getMetadataValue(UNROUTABLE_DEMAND.toString())).isEqualTo("1000.0");
        assertThat(getMetadataValue(SHARE_UNROUTABLE_DEMAND.toString())).isEqualTo("0.999000999000999");
        assertThat(getMetadataValue(LARGEST_UNROUTABLE_ZONE.toString())).isNull();
        assertThat(getMetadataValue(DEMAND_LARGEST_UNROUTABLE_ZONE.toString())).isNull();
        assertThat(getMetadataValue(VALID_AUTO.toString())).isEqualTo("false");
        assertThat(getMetadataValue(VALID_MANUAL.toString())).isNull();
    }

    @Test
    void testWriteWithNullLargestZone() throws Exception {
        // Create stats with no largest zone (empty unroutable demand)
        UnroutableDemand emptyDemand = new UnroutableDemand();
        Map<String, Double> statsMap = Map.of(ROUTED_DEMAND.toString(), 100.0);
        UnroutableDemandStats emptyStats = new UnroutableDemandStats(emptyDemand, statsMap, new Skim());

        // Write stats
        writer.write(emptyStats);

        // Verify each metadata value individually
        assertThat(getMetadataValue(UNROUTABLE_DEMAND.toString())).isEqualTo("0.0");
        assertThat(getMetadataValue(SHARE_UNROUTABLE_DEMAND.toString())).isEqualTo("0.0");
        assertThat(getMetadataValue(LARGEST_UNROUTABLE_ZONE.toString())).isNull();
        assertThat(getMetadataValue(DEMAND_LARGEST_UNROUTABLE_ZONE.toString())).isNull();
        assertThat(getMetadataValue(VALID_AUTO.toString())).isEqualTo("false");
        assertThat(getMetadataValue(VALID_MANUAL.toString())).isNull();
    }
} */
