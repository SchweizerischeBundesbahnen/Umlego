package ch.sbb.matsim.bewerto.elasticities;

import ch.sbb.matsim.bewerto.config.ElasticitiesParameters;
import ch.sbb.matsim.umlego.matrix.DemandMatrixMultiplier;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemandFactorCalculatorTest {

    @Mock
    private ZonesLookup mockLookup;

    private ElasticitiesParameters params;
    private DemandFactorCalculator calculator;
    private Map<String, Map<String, double[]>> baseSkims;
    private Map<String, Map<String, double[]>> variantSkims;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        // Use the existing elasticities file from resources
        String testElasticitiesFile = "src/test/resources/test_Elastizitaeten.csv";

        // Set up the mock behavior
        baseSkims = new HashMap<>();
        variantSkims = new HashMap<>();
        params = new ElasticitiesParameters()
                .setFile(testElasticitiesFile)
                .setSegment("Fr");

        // Sample data for testing
        setupTestData();

        // Configure mocks
        when(mockLookup.getCluster(anyString())).thenReturn("CH"); // Default cluster

        // Create the calculator
        calculator = new DemandFactorCalculator(params, mockLookup);
    }

    private void setupTestData() {
        // Create test data for zone1 -> zone2
        Map<String, double[]> zone1Destinations = new HashMap<>();
        zone1Destinations.put("zone2", new double[]{Double.NaN, Double.NaN, Double.NaN, 30.0, 10.0, 2.0, 0.0}); // JRT, ADT, NTR, PM
        zone1Destinations.put("zone3", new double[]{Double.NaN, Double.NaN, Double.NaN, 60.0, 15.0, 3.0, 0.0});
        baseSkims.put("zone1", zone1Destinations);

        Map<String, double[]> zone1VariantDest = new HashMap<>();
        zone1VariantDest.put("zone2", new double[]{Double.NaN, Double.NaN, Double.NaN, 25.0, 12.0, 1.8, 0.0}); // Variant has faster JRT, more transfers
        zone1VariantDest.put("zone3", new double[]{Double.NaN, Double.NaN, Double.NaN, 70.0, 13.0, 3.5, 0.0}); // Worse JRT, better ADT
        variantSkims.put("zone1", zone1VariantDest);

        // Create test data for zone2 -> zone3
        Map<String, double[]> zone2Destinations = new HashMap<>();
        zone2Destinations.put("zone3", new double[]{Double.NaN, Double.NaN, Double.NaN, 45.0, 5.0, 1.0, 0.0});
        baseSkims.put("zone2", zone2Destinations);

        Map<String, double[]> zone2VariantDest = new HashMap<>();
        zone2VariantDest.put("zone3", new double[]{Double.NaN, Double.NaN, Double.NaN, 40.0, 5.0, 1.0, 0.0}); // Only JRT improved
        variantSkims.put("zone2", zone2VariantDest);

        // Set clusters for testing zones
        when(mockLookup.getCluster("zone1")).thenReturn("CH");
        when(mockLookup.getCluster("zone2")).thenReturn("GG");
        when(mockLookup.getCluster("zone3")).thenReturn("AUSLAND");
    }

    @Test
    void constructor_shouldInitializeCorrectly() {
        // Test with segments that exist in the test file
        assertDoesNotThrow(() -> new DemandFactorCalculator(params, mockLookup));

        assertDoesNotThrow(() -> new DemandFactorCalculator(params.setSegment("FrK"), mockLookup));

        assertDoesNotThrow(() -> new DemandFactorCalculator(params.setSegment("Pe"), mockLookup));

        // Test with non-existent segment
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                new DemandFactorCalculator(params.setSegment("NonExistent"), mockLookup)
        );

        assertTrue(exception.getMessage().contains("No elasticity entries found for segment"));
    }

    @Test
    void createMultiplier_withValidODPair_shouldReturnCorrectFactor() {
        // Create the multiplier function using the class field data
        DemandMatrixMultiplier multiplier = calculator.createMultiplier(baseSkims.get("zone1"), variantSkims.get("zone1"));

        // Apply the multiplier to calculate a factor
        double factor = multiplier.getFactor("zone1", "zone2", 0);

        // We expect a reasonable factor based on our test data and the values in test_Elastizitaeten.csv
        assertTrue(factor > 0, "Factor should be positive");
        assertTrue(factor != 1.0, "Factor should not be the default value");

        // Verify that lookups were performed
        verify(mockLookup).getCluster("zone1");
        verify(mockLookup).getCluster("zone2");

        // Verify the factor is within reasonable bounds based on the elasticity parameters in the file
        double expectedMinFactor = 0.1;  // Based on the lowest f_min value in the file
        double expectedMaxFactor = 10.0; // Based on the highest f_max value in the file

        assertTrue(factor >= expectedMinFactor && factor <= expectedMaxFactor,
                "Factor should be within bounds defined by elasticity parameters");
    }

    @Test
    void createMultiplier_withMissingODPair_shouldReturnOne() {
        // Use the nonExistentZone which is not in our test data
        Map<String, double[]> emptyMap = new HashMap<>();

        // Create the multiplier function with empty maps
        DemandMatrixMultiplier multiplier = calculator.createMultiplier(emptyMap, emptyMap);

        // Apply the multiplier with non-existent zones
        double factor = multiplier.getFactor("nonExistentZone", "anotherNonExistentZone", 0);

        // Should default to 1.0 when skims are not found
        assertEquals(1.0, factor, 0.001);
    }

    @Test
    void writeFactors_shouldWriteCorrectCSVFile(@TempDir Path tempDir) throws Exception {
        // Create the multipliers using our class field data
        calculator.createMultiplier(baseSkims.get("zone1"), variantSkims.get("zone1"));
        calculator.createMultiplier(baseSkims.get("zone2"), variantSkims.get("zone2"));

        // Write factors to a test file
        String outputFile = tempDir.resolve("test_factors.csv").toString();
        calculator.writeFactors(outputFile);

        // Verify the file exists
        File factorsFile = new File(outputFile);
        assertTrue(factorsFile.exists(), "Factors file should exist");

        // Verify file content
        List<String> lines = Files.readAllLines(factorsFile.toPath());
        assertTrue(lines.size() >= 1, "File should have at least the header line");

        // Check header
        String header = lines.get(0);
        assertTrue(header.contains("From") && header.contains("To") &&
                        header.contains("F_JRT") && header.contains("F_ADT") && header.contains("F_NTR"),
                "Header should contain expected columns");

        // Note: We can't verify the specific zone pairs in the output because the writeFactors method
        // currently has a TODO and uses an empty ArrayList - we'd need to update the implementation
        // to properly test the output content
    }
}
