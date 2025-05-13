package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvDemandFolderMatrixParser extends AbstractCsvMatrixParser implements DemandMatricesParser  {

    private static final Logger LOG = LogManager.getLogger(CsvDemandFolderMatrixParser.class);

    private final String separator;

    public CsvDemandFolderMatrixParser(String path, ZonesLookup zonesLookup, double defaultValue, String separator) {
        super(path, zonesLookup, defaultValue);
        this.separator = separator;
    }

    /**
     * Parses multiple '.mtx' files in the specified directory to create an instance of DemandMatrices.
     * Each '.mtx' file represents a matrix of origin-destination pairs.
     * Retrieves CSV entries from each file using the specified separator.
     *
     * @return the DemandMatrices generated from the parsed CSV entries
     * @throws IOException if an error occurs while reading the files
     * @throws ZoneNotFoundException if a zone is not found during parsing
     */
    @Override
    public DemandMatrices parse() throws ZoneNotFoundException {
        try {
            File filePath = new File(getPath());
            Map<Integer, List<CSVEntry>> csvEntriesMap = new HashMap<>();
            File[] files = filePath.listFiles((dir, name) -> name.endsWith(".mtx"));
            if (files == null || files.length == 0) {
                throw new IllegalArgumentException("No '.mtx' files found in the directory");
            }
            for (File file : files) {
                int matrixIndex = parseMatrixIndex(file.getName());
                LOG.info("Parsing matrix file {}", file.getPath());
                List<CSVEntry> parsedMatrix = parseCsvMatrix(file.getPath(), separator);
                csvEntriesMap.put(matrixIndex, parsedMatrix);
            }
            return csvEntriesToDemandMatrices(csvEntriesMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int parseMatrixIndex(String name) {
        return Integer.parseInt(name.replaceAll(".*?(\\d{3}).*", "$1"));
    }
}
