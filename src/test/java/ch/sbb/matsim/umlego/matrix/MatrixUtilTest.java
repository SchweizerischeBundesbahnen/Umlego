package ch.sbb.matsim.umlego.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MatrixUtilTest {

    private Path tempDir;

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

}