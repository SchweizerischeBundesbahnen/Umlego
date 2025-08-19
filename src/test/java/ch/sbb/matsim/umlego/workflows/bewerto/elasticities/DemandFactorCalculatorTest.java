package ch.sbb.matsim.umlego.workflows.bewerto.elasticities;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.sbb.matsim.umlego.matrix.DemandMatrixMultiplier;
import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.matrix.ShareMatrix;
import ch.sbb.matsim.umlego.matrix.Zones;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import ch.sbb.matsim.umlego.workflows.bewerto.BewertoWorkResult;
import ch.sbb.matsim.umlego.workflows.bewerto.config.ElasticitiesParameters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DemandFactorCalculatorTest {

    @Mock
    private Zones mockLookup;

    private ElasticitiesParameters params;
    private DemandFactorCalculator calculator;
    private Map<String, Map<String, double[]>> baseSkims;
    private Map<String, Map<String, double[]>> variantSkims;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Use the existing elasticities file from resources
        String testElasticitiesFile = "src/test/resources/test_Elastizitaeten.csv";

        // Set up the mock behavior
        baseSkims = new HashMap<>();
        variantSkims = new HashMap<>();
        params = ElasticitiesParameters.builder()
            .file(testElasticitiesFile)
            .segment("Fr")
            .build();

        // Sample data for testing
        setupTestData();

        // Configure mocks
        when(mockLookup.getCluster(anyString())).thenReturn("CH"); // Default cluster

        double[][] shareMatrixData = {{1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}, {1.0, 1.0, 1.0}};
        var matrix = new ShareMatrix("Fr", shareMatrixData);

        Map<String, Integer> zoneLookupData = new HashMap<>();
        zoneLookupData.put("zone1", 0);
        zoneLookupData.put("zone2", 1);
        zoneLookupData.put("zone3", 2);
        var zonesLookUp = new ZonesLookup(zoneLookupData);

        var matrices = new Matrices(List.of(matrix), mockLookup, zonesLookUp);

        // Create the calculator
        calculator = new DemandFactorCalculator(params, matrices);
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
        var matrices = new Matrices(List.of(), mockLookup, null);
        assertThatCode(() -> new DemandFactorCalculator(params, matrices))
            .doesNotThrowAnyException();

        assertThatCode(() -> new DemandFactorCalculator(
            ElasticitiesParameters.builder()
                .file(params.getFile())
                .segment("FrK")
                .build()
            , matrices))
            .doesNotThrowAnyException();

        assertThatCode(() -> new DemandFactorCalculator(
            ElasticitiesParameters.builder()
                .file(params.getFile())
                .segment("Pe")
                .build()

            , matrices))
            .doesNotThrowAnyException();

    }

    @Test
    void createMultiplier_withValidODPair_shouldReturnCorrectFactor() {
        // Create the multiplier function using the class field data
        DemandMatrixMultiplier multiplier = calculator.createMultiplier(baseSkims.get("zone1"), variantSkims.get("zone1"));

        // Apply the multiplier to calculate a factor
        double factor = multiplier.getFactor("zone1", "zone2", 0);

        // We expect a reasonable factor based on our test data and the values in test_Elastizitaeten.csv
        assertThat(factor)
            .as("Factor should be positive")
            .isPositive()
            .as("Factor should not be the default value")
            .isNotEqualTo(1.0);

        // Verify that lookups were performed
        verify(mockLookup).getCluster("zone1");
        verify(mockLookup).getCluster("zone2");

        // Verify the factor is within reasonable bounds based on the elasticity parameters in the file
        double expectedMinFactor = 0.1;  // Based on the lowest f_min value in the file
        double expectedMaxFactor = 10.0; // Based on the highest f_max value in the file

        assertThat(factor)
            .as("Factor should be within bounds defined by elasticity parameters")
            .isBetween(expectedMinFactor, expectedMaxFactor);
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
        assertThat(factor).isCloseTo(1.0, within(0.001));
    }

    @Test
    void createMultiplier_createResult() throws Exception {
        // Create the multipliers using our class field data
        DemandFactorCalculator.Multiplier f1 = calculator.createMultiplier(baseSkims.get("zone1"), variantSkims.get("zone1"));
        DemandFactorCalculator.Multiplier f2 = calculator.createMultiplier(baseSkims.get("zone2"), variantSkims.get("zone2"));

        // Verify the multipliers are created correctly
        f1.getFactor("zone1", "zone2", 0);
        f1.getFactor("zone1", "zone3", 0);
        f2.getFactor("zone2", "zone3", 0);

        // Create result objects
        BewertoWorkResult zone1Result = f1.createResult("zone1");
        BewertoWorkResult zone2Result = f2.createResult("zone2");

        // Verify the results contain the correct data
        // 1. Check origin zones
        assertThat(zone1Result.originZone()).isEqualTo("zone1");
        assertThat(zone2Result.originZone()).isEqualTo("zone2");

        // 2. Check the factors map contains the expected destination zones
        Map<String, double[]> zone1Factors = zone1Result.factors();
        assertThat(zone1Factors)
            .as("Result should contain factors for zone2 and zone3")
            .containsKeys("zone2", "zone3");

        Map<String, double[]> zone2Factors = zone2Result.factors();
        assertThat(zone2Factors)
            .as("Result should contain factors for zone3")
            .containsKey("zone3");

        // 3. Check the individual factors (JRT, ADT, NTR) for each zone pair
        double[] zone1ToZone2Factors = zone1Factors.get("zone2");
        double[] zone1ToZone3Factors = zone1Factors.get("zone3");
        double[] zone2ToZone3Factors = zone2Factors.get("zone3");

        // Verify zone1 -> zone2 factors
        assertThat(zone1ToZone2Factors)
            .as("Factors for zone1->zone2 should not be null")
            .isNotNull()
            .as("Should have 3 factor values (JRT, ADT, NTR)")
            .hasSize(3);

        // Verify zone1 -> zone3 factors
        assertThat(zone1ToZone3Factors)
            .as("Factors for zone1->zone3 should not be null")
            .isNotNull()
            .as("Should have 3 factor values")
            .hasSize(3);

        // Verify zone2 -> zone3 factors (only JRT improved)
        assertThat(zone2ToZone3Factors)
            .as("Factors for zone2->zone3 should not be null")
            .isNotNull()
            .as("Should have 3 factor values")
            .hasSize(3);

    }
}
