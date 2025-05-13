package ch.sbb.matsim.umlego.matrix;

import ch.sbb.matsim.umlego.readers.jdbc.TimesliceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

class MatrixUtilTest {


    private Path tempDir;
    private ZonesLookup zonesLookup;

    private static Path createZonesLookup(Path filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filePath.toFile()))) {
            writer.println("NAME;NO");
            writer.println("zone1;0");
            writer.println("zone2;1");
        }
        return filePath;
    }

    @BeforeEach
    void setUp() {
        try {
            System.setProperty("LOCAL", "true");
            tempDir = Files.createTempDirectory("matrices");
            zonesLookup = new ZonesLookup(createZonesLookup(tempDir.resolve("lookup.csv")).toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void matrixNameToMinutes() {
        assertEquals(0, MatrixUtil.matrixNameToMinutes("1"));
        assertEquals(1430, MatrixUtil.matrixNameToMinutes("144"));
    }

    @Test
    void matrixIndexToMinutes() {
        assertEquals(10, MatrixUtil.matrixIndexToMinutes(1));
    }

    @Test
    void minutesToMatrixIndex() {
        assertEquals(2, MatrixUtil.minutesToMatrixIndex(20));
    }

    @Test
    void minutesToMatrixName() {
        assertEquals("1", MatrixUtil.minutesToMatrixName(0));
    }

    @Test
    void testConvertToDataArray_withValidZones() throws ZoneNotFoundException {

        // Prepare test data
        List<TimesliceRepository.TimesliceJdbcEntry> entries = List.of(
                new TimesliceRepository.TimesliceJdbcEntry(1, "zone1", "zone2", 3.5)
        );

        double[][] result = MatrixUtil.convertToDataArrayJdbc(zonesLookup, entries, true, 0.0);

        // Validate results
        assertEquals(3.5, result[0][1]);
        assertEquals(0.0, result[1][0]);
    }

    @Test
    void testConvertToDataArray_withUnknownZone_ignoreExcessZones() throws ZoneNotFoundException {

        // Prepare test data
        List<TimesliceRepository.TimesliceJdbcEntry> entries = List.of(
                new TimesliceRepository.TimesliceJdbcEntry(1, "zone1", "zone3", 3.5)
        );

        // Execute the method with ignoreExcessZones = true
        double[][] result = MatrixUtil.convertToDataArrayJdbc(zonesLookup, entries, true, 0.0);

        // Validate that the entry was ignored
        assertEquals(0.0, result[0][0]);
        assertEquals(0.0, result[0][1]);
    }
}