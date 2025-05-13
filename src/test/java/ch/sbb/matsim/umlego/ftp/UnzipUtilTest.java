package ch.sbb.matsim.umlego.ftp;

import static ch.sbb.matsim.umlego.ftp.UnzipUtil.unzipFile;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnzipUtilTest {

    private static final String TEST_ZIP_PATH = "src/test/resources/test.zip";
    private static final String UNZIP_DESTINATION = "src/test/resources/unzipped_test_files";

    @BeforeEach
    void setUp() {
        new File(UNZIP_DESTINATION).mkdirs();
    }

    @AfterEach
    void tearDown() throws IOException {
        try (Stream<Path> files = Files.walk(Paths.get(UNZIP_DESTINATION))) {
            files.map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    void testUnzipFile() {
        File zipFile = new File(TEST_ZIP_PATH);
        if (!zipFile.exists()) {
            fail("Test zip file not found: " + TEST_ZIP_PATH);
        }

        try {
            unzipFile(zipFile, UNZIP_DESTINATION);

            try (Stream<Path> files = Files.walk(Paths.get(UNZIP_DESTINATION))) {
                files.forEach(System.out::println);
            }

            Path expectedFile1 = Paths.get(UNZIP_DESTINATION, "test1.txt");
            Path expectedFile2 = Paths.get(UNZIP_DESTINATION, "test2.txt");
            Path expectedFile3 = Paths.get(UNZIP_DESTINATION, "test2.txt");

            assertTrue(Files.exists(expectedFile1), "test1.txt should exist after unzipping");
            assertTrue(Files.exists(expectedFile2), "test2.txt should exist after unzipping");
            assertTrue(Files.exists(expectedFile3), "test3.txt should exist after unzipping");

        } catch (IOException e) {
            fail("IOException during testUnzipFile: " + e.getMessage());
        }
    }
}



