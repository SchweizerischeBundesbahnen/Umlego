package ch.sbb.matsim.bewerto.elasticities;

import ch.sbb.matsim.bewerto.config.ElasticitiesParameters;
import ch.sbb.matsim.umlego.UmlegoSkimCalculator;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import ch.sbb.matsim.umlego.writers.types.skim.ODPair;
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

    @Mock
    private UmlegoSkimCalculator mockBaseSkim;

    @Mock
    private UmlegoSkimCalculator mockVariantSkim;

    private ElasticitiesParameters params;
    private DemandFactorCalculator calculator;
    private Map<ODPair, double[]> baseSkims;
    private Map<ODPair, double[]> variantSkims;

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
        when(mockBaseSkim.getSkims()).thenReturn(baseSkims);
        when(mockVariantSkim.getSkims()).thenReturn(variantSkims);
        when(mockLookup.getCluster(anyString())).thenReturn("CH"); // Default cluster

        // Create the calculator
        calculator = new DemandFactorCalculator(params, mockLookup, mockBaseSkim, mockVariantSkim);
    }

    private void setupTestData() {

        // Create test data for a few OD pairs
        ODPair od1 = new ODPair("zone1", "zone2");
        baseSkims.put(od1, new double[]{Double.NaN, Double.NaN, Double.NaN, 30.0, 10.0, 2.0, 0.0}); // JRT, ADT, NTR, PM
        variantSkims.put(od1, new double[]{Double.NaN, Double.NaN, Double.NaN, 25.0, 12.0, 1.8, 0.0}); // Variant has faster JRT, more transfers

        ODPair od2 = new ODPair("zone2", "zone3");
        baseSkims.put(od2, new double[]{Double.NaN, Double.NaN, Double.NaN, 45.0, 5.0, 1.0, 0.0});
        variantSkims.put(od2, new double[]{Double.NaN, Double.NaN, Double.NaN, 40.0, 5.0, 1.0, 0.0}); // Only JRT improved

        ODPair od3 = new ODPair("zone1", "zone3");
        baseSkims.put(od3, new double[]{Double.NaN, Double.NaN, Double.NaN, 60.0, 15.0, 3.0, 0.0});
        variantSkims.put(od3, new double[]{Double.NaN, Double.NaN, Double.NaN, 70.0, 13.0, 3.5, 0.0}); // Worse JRT, better ADT

        // Set clusters for testing zones
        when(mockLookup.getCluster("zone1")).thenReturn("CH");
        when(mockLookup.getCluster("zone2")).thenReturn("GG");
        when(mockLookup.getCluster("zone3")).thenReturn("AUSLAND");
    }

    @Test
    void constructor_shouldInitializeCorrectly() {
        // Test with segments that exist in the test file
        assertDoesNotThrow(() -> new DemandFactorCalculator(params, mockLookup, mockBaseSkim, mockVariantSkim));

        assertDoesNotThrow(() -> new DemandFactorCalculator(params.setSegment("FrK"), mockLookup, mockBaseSkim, mockVariantSkim));

        assertDoesNotThrow(() -> new DemandFactorCalculator(params.setSegment("Pe"), mockLookup, mockBaseSkim, mockVariantSkim));

        // Test with non-existent segment
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                new DemandFactorCalculator(params.setSegment("NonExistent"), mockLookup, mockBaseSkim, mockVariantSkim)
        );

        assertTrue(exception.getMessage().contains("No elasticity entries found for segment"));

    }

    @Test
    void calculateFactor_withValidODPair_shouldReturnCorrectFactor() {
        // Test with a valid OD pair
        double factor = calculator.calculateFactor("zone1", "zone2");

        // We expect a reasonable factor based on our test data and the values in test_Elastizitaeten.csv
        assertTrue(factor > 0, "Factor should be positive");
        assertTrue(factor != 1.0, "Factor should not be the default value");

        // Verify that lookups were performed
        verify(mockBaseSkim).getSkims();
        verify(mockVariantSkim).getSkims();
        verify(mockLookup).getCluster("zone1");

        // Verify the factor is within reasonable bounds based on the elasticity parameters in the file
        // For cluster 1 and Fr segment, elasticities are quite large in the test file
        // So we expect significant changes in the factor when travel times change
        double expectedMinFactor = 0.1;  // Based on the lowest f_min value in the file
        double expectedMaxFactor = 10.0; // Based on the highest f_max value in the file

        assertTrue(factor >= expectedMinFactor && factor <= expectedMaxFactor,
                "Factor should be within bounds defined by elasticity parameters");
    }

    @Test
    void calculateFactor_withMissingODPair_shouldReturnOne() {
        // Test with a non-existent OD pair
        double factor = calculator.calculateFactor("nonExistentZone", "anotherNonExistentZone");

        // Should default to 1.0 when skims are not found
        assertEquals(1.0, factor, 0.001);
    }

    @Test
    void writeFactors_shouldWriteCorrectCSVFile(@TempDir Path tempDir) throws Exception {
        // Calculate a few factors to populate the internal maps
        calculator.calculateFactor("zone1", "zone2");
        calculator.calculateFactor("zone2", "zone3");
        calculator.calculateFactor("zone1", "zone3");

        // Write factors to a test file
        String outputFile = tempDir.resolve("test_factors.csv").toString();
        calculator.writeFactors(outputFile);

        // Verify the file exists
        File factorsFile = new File(outputFile);
        assertTrue(factorsFile.exists(), "Factors file should exist");

        // Verify file content
        List<String> lines = Files.readAllLines(factorsFile.toPath());
        assertTrue(lines.size() >= 4, "File should have at least 4 lines (header + 3 data rows)");

        // Check header
        String header = lines.get(0);
        assertTrue(header.contains("From") && header.contains("To") &&
                        header.contains("F_JRT") && header.contains("F_ADT") && header.contains("F_NTR"),
                "Header should contain expected columns");

        // Check that all zone pairs are included
        boolean foundZone1ToZone2 = false;
        boolean foundZone2ToZone3 = false;
        boolean foundZone1ToZone3 = false;

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("zone1") && line.contains("zone2")) foundZone1ToZone2 = true;
            if (line.contains("zone2") && line.contains("zone3")) foundZone2ToZone3 = true;
            if (line.contains("zone1") && line.contains("zone3")) foundZone1ToZone3 = true;
        }

        assertTrue(foundZone1ToZone2, "Output should include zone1 to zone2");
        assertTrue(foundZone2ToZone3, "Output should include zone2 to zone3");
        assertTrue(foundZone1ToZone3, "Output should include zone1 to zone3");
    }
}
