package ch.sbb.matsim.umlego.demand.json;

import static org.assertj.core.api.Assertions.assertThat;

import ch.sbb.matsim.umlego.config.UmlegoConfig;
import ch.sbb.matsim.umlego.demand.UnroutableDemand;
import ch.sbb.matsim.umlego.demand.UnroutableDemandPart;
import ch.sbb.matsim.umlego.demand.UnroutableDemandStats;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.DemandMatrix;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class JsonUnroutableDemandStatsWriterTest {

    @TempDir
    Path tempDir;
    
    private JsonUnroutableDemandStatsWriter writer;
    private UnroutableDemandStats stats;
    private static MockedStatic<UmlegoConfig> mockedConfig;

    @BeforeEach
    void setUp() {
        // Mock UmlegoConfig.isRunningLocally() to always return true
        mockedConfig = Mockito.mockStatic(UmlegoConfig.class);
        mockedConfig.when(UmlegoConfig::isRunningLocally).thenReturn(true);
        
        writer = new JsonUnroutableDemandStatsWriter(tempDir.toString());
        
        // Create test data with high unroutable demand to exceed 0.95 limit
        UnroutableDemand unroutableDemand = new UnroutableDemand();
        unroutableDemand.addPart(new UnroutableDemandPart("zone1", "zone2", 950.0));
        unroutableDemand.addPart(new UnroutableDemandPart("zone1", "zone3", 50.0));

        ZonesLookup lookup = new ZonesLookup(
            Map.of("1", 0));
        double[][] data = new double[1][1];
        data[0][0] = 1001.0;
        DemandMatrix matrix = new DemandMatrix(0, 15, data);
        DemandMatrices demandMatrices = new DemandMatrices(List.of(matrix), lookup);

        stats = new UnroutableDemandStats(unroutableDemand, demandMatrices);
    }

    @AfterEach
    void tearDown() {
        if (mockedConfig != null) {
            mockedConfig.close();
        }
    }

    void testWriteUnroutableDemandStats() throws IOException {
        // Write stats
        writer.write(stats);
        
        // Read the written file
        Path outputPath = tempDir.resolve("unroutable_stats.json");
        String jsonContent = Files.readString(outputPath).replaceAll("\\s+", "");
        
        // Expected JSON with exact values
        String expected = """
            {"LargestUnroutableZone":"","UnroutableDemand":1000,"ShareUnroutableDemand":0.999000999000999,"DemandLargestUnroutableZone":0}
            """.replaceAll("\\s+", "");
        
        // Verify exact JSON content
        assertThat(jsonContent).isEqualTo(expected);
    }

    void testWriteWithNullLargestZone() throws IOException {
        // Create stats with no largest zone (empty unroutable demand)
        UnroutableDemand emptyDemand = new UnroutableDemand();
        UnroutableDemandStats emptyStats = new UnroutableDemandStats(emptyDemand, new DemandMatrices());
        
        // Write stats
        writer.write(emptyStats);
        
        // Read the written file
        Path outputPath = tempDir.resolve("unroutable_stats.json");
        String jsonContent = Files.readString(outputPath).replaceAll("\\s+", "");
        
        // Expected JSON with exact values
        String expected = """
            {"LargestUnroutableZone":"","UnroutableDemand":0,"ShareUnroutableDemand":0,"DemandLargestUnroutableZone":0}
            """.replaceAll("\\s+", "");
        
        // Verify exact JSON content
        assertThat(jsonContent).isEqualTo(expected);
    }
} 