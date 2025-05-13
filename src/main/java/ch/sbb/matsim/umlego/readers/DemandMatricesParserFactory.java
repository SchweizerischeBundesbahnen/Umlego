package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.ZonesLookup;
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

    public static DemandMatricesParser createParser(String filePath, ZonesLookup zonesLookup) throws IOException {
        LOG.info("Reading demand matrices from files in {}", filePath);
        return createParserForFile(filePath, zonesLookup);
    }

    private static DemandMatricesParser createParserForFile(String filePath, ZonesLookup zonesLookup) throws IOException {
        if (filePath.endsWith(".csv")) {
            return new CsvMultiMatrixDemandParser(filePath, zonesLookup, 1, ",");
        } else if (new File(filePath).isDirectory()) {
            return new CsvDemandFolderMatrixParser(filePath, zonesLookup, 0, "\\s+");
        } else if (filePath.endsWith(".omx")) {
            return new OmxMatrixParser(filePath, zonesLookup);
        } else {
            throw new IOException("Unsupported file format: " + filePath);
        }
    }

}
