package ch.sbb.matsim.umlego.matrix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import ch.sbb.matsim.umlego.readers.CsvFactorMatrixParser;
import ch.sbb.matsim.umlego.readers.CsvFolderMatrixParser;
import ch.sbb.matsim.umlego.readers.CsvMultiMatrixParser;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class MatricesTest {

    private final Path tempDir;
    private final String mtxPath;
    private final String multiMtxPath;
    private final Zones zones;

    public MatricesTest() throws IOException {
        System.setProperty("LOCAL", "true");
        tempDir = Files.createTempDirectory("matrices");
        mtxPath = createCSVFile(tempDir.resolve("matrix000.mtx")).toString();
        createSparseCSVFile(tempDir.resolve("matrix001.mtx"));
        multiMtxPath = createCSVFileWithTypeCol(tempDir.resolve("multi_matrix.csv")).toString();
        zones = new Zones(createZonesLookup(tempDir.resolve("lookup.csv")).toString());
    }

    @Test
    void testOpenMatrix() throws ZoneNotFoundException {
        CsvFolderMatrixParser parser = new CsvFolderMatrixParser(tempDir.toString(), zones, 0, "\\s+");
        Matrices matrices = parser.parse();

        List<String> matrixNames = matrices.getMatrixNames();
        assertEquals(2, matrixNames.size());
        assertTrue(matrixNames.contains("1"));
        assertTrue(matrixNames.contains("2"));

        List<String> zoneIds = zones.getAllZoneNos();
        assertEquals(3, zoneIds.size());
        assertTrue(zoneIds.contains("1"));
        assertTrue(zoneIds.contains("2"));
        assertTrue(zoneIds.contains("3"));
        assertEquals(1, matrices.getDemandMatrices().stream().map(m -> m.getData().length).distinct().count());
    }

    @Test
    void testGetDemandMatrixValue() throws IOException, ZoneNotFoundException {
        var lookup = zones.createDefaultZonesLookup();
        CsvFactorMatrixParser parser = new CsvFactorMatrixParser(mtxPath, zones, 1, "\\s+", lookup);
        FactorMatrix factorMatrix = parser.parseFactorMatrix();

        double value = factorMatrix.getValue(lookup.getIndex("1"), lookup.getIndex("2"));
        assertEquals(3.0, value, 0.0);

        double value2 = factorMatrix.getValue(lookup.getIndex("2"), lookup.getIndex("1"));
        assertEquals(2.0, value2, 0.0);
    }

    @Test
    void testOpenMatrixWithTypeCol() throws ZoneNotFoundException {
        CsvMultiMatrixParser parser = new CsvMultiMatrixParser(multiMtxPath, zones, 1, ",");
        Matrices matrices = parser.parse();

        List<String> matrixNames = matrices.getMatrixNames();
        assertEquals(2, matrixNames.size());

        double value = matrices.getMatrixValue("1", "2", new TimeWindow(20, 30));
        assertEquals(0.3, value, 0.0);
    }

    @Test
    void testMultiplyAllMatricesWithTarget() throws IOException, ZoneNotFoundException {
        var lookup = zones.createDefaultZonesLookup();
        CsvFactorMatrixParser parser = new CsvFactorMatrixParser(mtxPath, zones, 1, "\\s+", lookup);
        FactorMatrix baseDemand = parser.parseFactorMatrix();

        CsvMultiMatrixParser parser2 = new CsvMultiMatrixParser(multiMtxPath, zones, 0, ",", baseDemand);
        Matrices matrices = parser2.parse();

        // The zoneLookup has an additional zone, which is filled with defaultValue
        double[][] expectedMatrix1 = {{0.1, 0.6, 0.0}, {0.4, 0.4, 0.0}, {0.0, 0.0, 0.0}};
        double[][] expectedMatrix2 = {{0.4, 0.9, 0.0}, {0.6, 1.6, 0.0}, {0.0, 0.0, 0.0}};

        assertMatricesEqual(expectedMatrix1, matrices.getDemandMatrix(new TimeWindow(10 , 20  )).getData());
        assertMatricesEqual(expectedMatrix2, matrices.getDemandMatrix(new TimeWindow(20 , 30 )).getData());
    }

    private static Path createCSVFile(Path filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
            writer.println("from,to,value");
            writer.println("1 1 1.0");
            writer.println("1 2 3.0");
            writer.println("2 1 2.0");
            writer.println("2 2 4.0");
        }

        return filePath;
    }

    private static Path createSparseCSVFile(Path filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
            writer.println("from,to,value");
            writer.println("3 3 0.0");
        }
        return filePath;
    }

    private Path createCSVFileWithTypeCol(Path filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
            writer.println("from,to,name,value");
            writer.println("1,1,1,0.1");
            writer.println("2,2,1,0.1");
            writer.println("1,2,1,0.2");
            writer.println("2,1,1,0.2");
            writer.println("1,1,2,0.4");
            writer.println("2,2,2,0.4");
            writer.println("1,2,2,0.3");
            writer.println("2,1,2,0.3");
        }
        return filePath;
    }

    private static Path createZonesLookup(Path filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
            writer.println("NAME;NO");
            writer.println("1;1");
            writer.println("2;2");
            writer.println("3;3");
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
