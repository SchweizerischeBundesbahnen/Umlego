package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.Zones;
import java.io.File;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Factory creating CVS or Database parser for reading and parsing the demand matrices.
 */
public class DemandMatricesParserFactory {

    private static final Logger LOG = LogManager.getLogger(DemandMatricesParserFactory.class);

    private DemandMatricesParserFactory() {
    }

    public static MatricesParser createParser(String filePath, Zones zones, MatrixFactory matrixFactory) throws IOException {
        LOG.info("Reading demand matrices from files in {}", filePath);
        return createParserForFile(filePath, zones, matrixFactory);
    }

    private static MatricesParser createParserForFile(String filePath, Zones zones, MatrixFactory matrixFactory) throws IOException {
        if (filePath.endsWith(".csv")) {
            return new CsvMultiMatrixParser(filePath, zones, 1, ",");
        } else if (new File(filePath).isDirectory()) {
            return new CsvFolderMatrixParser(filePath, zones, 0, "\\s+");
        } else if (filePath.endsWith(".omx")) {
            return new OmxMatrixParser(filePath, zones, matrixFactory);
        } else {
            throw new IOException("Unsupported file format: " + filePath);
        }
    }

}
