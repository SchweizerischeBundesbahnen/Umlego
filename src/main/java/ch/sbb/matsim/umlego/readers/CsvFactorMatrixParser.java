package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.FactorMatrix;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.Zones;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import com.google.common.io.Files;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CsvFactorMatrixParser extends AbstractCsvMatrixParser {

    private static final Logger LOG = LogManager.getLogger(CsvFactorMatrixParser.class);

    private final String separator;
    private ZonesLookup zonesLookup;

    public CsvFactorMatrixParser(String filename, Zones zones, double defaultValue, String separator, ZonesLookup zonesLookup) {
        super(filename, zones, defaultValue);
        this.separator = separator;
        this.zonesLookup = zonesLookup;
    }

    /**
     * Parses a CSV file to create a FactorMatrix object. Expected columns are "from", "to", and "value".
     *
     * @return a FactorMatrix object created from the parsed CSV data
     * @throws IOException if an error occurs while reading the file
     * @throws ZoneNotFoundException if a zone ID in the CSV entries is not found
     */
    public FactorMatrix parseFactorMatrix() throws IOException, ZoneNotFoundException {
        LOG.info("Parsing matrix file " + getPath());
        BufferedReader bufferedReader = new BufferedReader(new FileReader(getPath()));
        List<CSVEntry> csvEntries = parseCsvMatrix(bufferedReader, this.separator);
        double[][] data = convertToDataArray(csvEntries, true, this.zonesLookup);
        String name = Files.getNameWithoutExtension(getPath());
        return new FactorMatrix(data, name);
    }
}
