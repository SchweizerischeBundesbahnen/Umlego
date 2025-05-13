package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import ch.sbb.matsim.umlego.readers.jdbc.JdbcDemandMatrixParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * Factory creating CVS or Database parser for reading and parsing the demand matrices.
 *
 */
public class DemandMatricesParserFactory {

    private static final Logger LOG = LogManager.getLogger(DemandMatricesParserFactory.class);

    private DemandMatricesParserFactory() {}

    public static DemandMatricesParser createParser(String filePath, String simbaRunId, String saison, ZonesLookup zonesLookup) throws IOException {
        if (filePath != null) {
            LOG.info("Reading demand matrices from files in {}", filePath);
            return createParserForFile(filePath, zonesLookup);
        } else {
            LOG.info("Reading demand matrices from database run {} {}", simbaRunId, saison);
            return createParserForDatabase(simbaRunId, saison, zonesLookup);
        }
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

    private static DemandMatricesParser createParserForDatabase(String simbaRunId, String saison, ZonesLookup zonesLookup) {
        return new JdbcDemandMatrixParser(simbaRunId, saison, zonesLookup, 0);
    }
}
