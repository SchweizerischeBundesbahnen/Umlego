package ch.sbb.matsim.umlego.readers;

import static ch.sbb.matsim.umlego.matrix.MatrixUtil.matrixNameToMinutes;
import static ch.sbb.matsim.umlego.matrix.MatrixUtil.minutesToMatrixIndex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import ch.sbb.matsim.umlego.config.MatricesParameters;
import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.matrix.DemandMatrix;
import ch.sbb.matsim.umlego.matrix.Zone;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.Zones;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import omx.OmxFile;
import omx.OmxMatrix;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class OmxMatrixParserTest {

    private static final String TEST_OMX_FILE = "input_data/demand/demand_matrices.omx";

    @Disabled("This test required demand file and native library. Only used during development.")
    @Test
    void testOmxParsingComparison() throws ZoneNotFoundException {

        // Parse using the original OmxFile implementation to get matrices
        OmxFile legacyOmxFile = new OmxFile(TEST_OMX_FILE);
        legacyOmxFile.openReadOnly();

        // Create a zones lookup for testing - needed for both implementations
        int[] externalNumbers = (int[]) legacyOmxFile.getLookup("NO").getLookup();

        List<Zone> zoneData = new ArrayList<>();
        for (int i = 0; i < externalNumbers.length; i++) {
            zoneData.add(new Zone(String.valueOf(externalNumbers[i]), "", "CH"));
        }

        var zones = new Zones(zoneData);

        // Get all matrices from the official OMX implementation
        Map<Integer, double[][]> legacyMatrices = new HashMap<>();
        for (String name : legacyOmxFile.getMatrixNames()) {
            OmxMatrix.OmxDoubleMatrix matrix = (OmxMatrix.OmxDoubleMatrix) legacyOmxFile.getMatrix(name);
            // Same index as in the demand matrices class
            int idx = minutesToMatrixIndex(matrixNameToMinutes(name));
            legacyMatrices.put(idx, matrix.getData());
        }
        legacyOmxFile.close();


        MatricesParameters matricesParameters = new MatricesParameters("", "", "", List.of(), List.of());
        MatrixFactory matrixFactory = new MatrixFactory(matricesParameters);

        // Parse using the new jhdf implementation
        OmxMatrixParser parser = new OmxMatrixParser(TEST_OMX_FILE, zones, matrixFactory);
        Matrices matrices = parser.parse();
        List<DemandMatrix> newMatrices = matrices.getDemandMatrices();

        // Verify that matrices exist
        assertNotNull(newMatrices);
        assertThat(newMatrices).isNotEmpty();

        // Verify the number of matrices matches
        assertEquals(legacyMatrices.size(), newMatrices.size(), "Number of matrices should match");

        // Compare the matrix data between the two implementations
        for (Integer matrixName : legacyMatrices.keySet()) {

            double[][] legacyData = legacyMatrices.get(matrixName);
            double[][] newData = newMatrices.get(matrixName).getData();

            // Verify dimensions match
            assertEquals(legacyData.length, newData.length,
                "Number of rows should match for matrix " + matrixName);
            assertEquals(legacyData[0].length, newData[0].length,
                "Number of columns should match for matrix " + matrixName);

            // Compare all cells in the matrix
            for (int row = 0; row < legacyData.length; row++) {
                for (int col = 0; col < legacyData[row].length; col++) {
                    assertEquals(legacyData[row][col], newData[row][col], 1e-10,
                        "Cell values should match at [" + row + "][" + col + "] for matrix " + matrixName);
                }
            }
        }
    }
}
