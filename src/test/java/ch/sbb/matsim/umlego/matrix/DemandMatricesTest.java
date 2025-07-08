package ch.sbb.matsim.umlego.matrix;

import ch.sbb.matsim.umlego.readers.CsvDemandFolderMatrixParser;
import ch.sbb.matsim.umlego.readers.CsvFactorMatrixParser;
import ch.sbb.matsim.umlego.readers.CsvMultiMatrixDemandParser;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class DemandMatricesTest {

    private final Path tempDir;
    private final String mtxPath;
    private final String multiMtxPath;
    private final Zones zones;

    public DemandMatricesTest() throws IOException {
        System.setProperty("LOCAL", "true");
        tempDir = Files.createTempDirectory("matrices");
        mtxPath = createCSVFile(tempDir.resolve("matrix000.mtx")).toString();
        createSparseCSVFile(tempDir.resolve("matrix001.mtx"));
        multiMtxPath = createCSVFileWithTypeCol(tempDir.resolve("multi_matrix.csv")).toString();
        zones = new Zones(createZonesLookup(tempDir.resolve("lookup.csv")).toString());
    }

    @Test
    void testOpenMatrix() throws ZoneNotFoundException {
        CsvDemandFolderMatrixParser parser = new CsvDemandFolderMatrixParser(tempDir.toString(), zones, 0, "\\s+");
        DemandMatrices demandMatrices = parser.parse();

        List<String> matrixNames = demandMatrices.getMatrixNames();
        assertEquals(2, matrixNames.size());
        assertTrue(matrixNames.contains("1"));
        assertTrue(matrixNames.contains("2"));

        List<String> zoneIds = zones.getAllNos();
        assertEquals(3, zoneIds.size());
        assertTrue(zoneIds.contains("zone1"));
        assertTrue(zoneIds.contains("zone2"));
        assertTrue(zoneIds.contains("zone3"));
        assertEquals(1, demandMatrices.getMatrices().values().stream().map(m -> m.getData().length).distinct().count());
    }

    @Test
    void testGetMatrixValue() throws IOException, ZoneNotFoundException {
        CsvFactorMatrixParser parser = new CsvFactorMatrixParser(mtxPath, zones, 1, "\\s+");
        FactorMatrix factorMatrix = parser.parseFactorMatrix();

        double value = factorMatrix.getValue(zones.getNo("zone1"), zones.getNo("zone2"));
        assertEquals(3.0, value, 0.0);

        double value2 = factorMatrix.getValue(zones.getNo("zone2"), zones.getNo("zone1"));
        assertEquals(2.0, value2, 0.0);
    }

    @Test
    void testOpenMatrixWithTypeCol() throws ZoneNotFoundException {
        CsvMultiMatrixDemandParser parser = new CsvMultiMatrixDemandParser(multiMtxPath, zones, 1, ",");
        DemandMatrices matrices = parser.parse();

        List<String> matrixNames = matrices.getMatrixNames();
        assertEquals(2, matrixNames.size());

        double value = matrices.getMatrixValue("zone1", "zone2", "3");
        assertEquals(0.3, value, 0.0);
    }

    @Test
    void testMultiplyAllMatricesWithTarget() throws IOException, ZoneNotFoundException {
        CsvFactorMatrixParser parser = new CsvFactorMatrixParser(mtxPath, zones, 1, "\\s+");
        FactorMatrix baseDemand = parser.parseFactorMatrix();

        CsvMultiMatrixDemandParser parser2 = new CsvMultiMatrixDemandParser(multiMtxPath, zones, 0, ",", baseDemand);
        DemandMatrices matrices = parser2.parse();

        // The zoneLookup has an additional zone, which is filled with defaultValue
        double[][] expectedMatrix1 = {{0.1, 0.6, 0.0}, {0.4, 0.4, 0.0}, {0.0, 0.0, 0.0}};
        double[][] expectedMatrix2 = {{0.4, 0.9, 0.0}, {0.6, 1.6, 0.0}, {0.0, 0.0, 0.0}};

        assertMatricesEqual(expectedMatrix1, matrices.getMatrix(13).getData());
        assertMatricesEqual(expectedMatrix2, matrices.getMatrix(28).getData());
    }

    private static Path createCSVFile(Path filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
            writer.println("from,to,value");
            writer.println("zone1 zone1 1.0");
            writer.println("zone1 zone2 3.0");
            writer.println("zone2 zone1 2.0");
            writer.println("zone2 zone2 4.0");
        }

        return filePath;
    }

    private static Path createSparseCSVFile(Path filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
            writer.println("from,to,value");
            writer.println("zone3 zone3 0.0");
        }
        return filePath;
    }

    private Path createCSVFileWithTypeCol(Path filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
            writer.println("from,to,name,value");
            writer.println("zone1,zone1,1,0.1");
            writer.println("zone2,zone2,1,0.1");
            writer.println("zone1,zone2,1,0.2");
            writer.println("zone2,zone1,1,0.2");
            writer.println("zone1,zone1,2,0.4");
            writer.println("zone2,zone2,2,0.4");
            writer.println("zone1,zone2,2,0.3");
            writer.println("zone2,zone1,2,0.3");
        }
        return filePath;
    }

    private static Path createZonesLookup(Path filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
            writer.println("NAME;NO");
            writer.println("zone1;0");
            writer.println("zone2;1");
            writer.println("zone3;2");
        }
        return filePath;
    }

    private void assertMatricesEqual(double[][] expected, double[][] actual) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i], 0.0001);
        }
    }
}
