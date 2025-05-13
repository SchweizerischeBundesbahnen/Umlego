package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.FactorMatrix;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvMultiMatrixDemandParser extends AbstractCsvMatrixParser implements DemandMatricesParser {

    private final String separator;
    private final FactorMatrix baseDemand;

    public CsvMultiMatrixDemandParser(String path, ZonesLookup zonesLookup, double defaultValue, String separator, FactorMatrix baseDemand) {
        super(path, zonesLookup, defaultValue);
        this.separator = separator;
        this.baseDemand = baseDemand;
    }

    public CsvMultiMatrixDemandParser(String path, ZonesLookup zonesLookup, double defaultValue, String separator) {
        this(path, zonesLookup, defaultValue, separator, null);
    }

    /**
     * Parses a CSV file containing multiple matrices data to generate DemandMatrices. The CSV-separator is specified in the constructor.
     *
     * The CSV file is expected to have the following columns: from, to, matrixIndex and value.
     *
     * @return the DemandMatrices generated from the parsed CSV data
     * @throws ZoneNotFoundException if a zone is not found during the parsing process
     */
    @Override
    public DemandMatrices parse() throws ZoneNotFoundException {
        try {
            Map<Integer, List<CSVEntry>> csvEntries = parseCsvMultiMatrix(getPath(), this.separator);
            DemandMatrices demandMatrices = csvEntriesToDemandMatrices(csvEntries);
            if (baseDemand != null) {
                demandMatrices.multiplyWith(baseDemand);
            }
            return demandMatrices;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<Integer, List<CSVEntry>> parseCsvMultiMatrix(String filePath, String separator) throws IOException {
        Map<Integer, List<CSVEntry>> csvEntries = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        br.readLine(); // skip header
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.trim().split(separator);
            csvEntries.computeIfAbsent(Integer.parseInt(parts[2]), matrixName -> new ArrayList<>()).add(
                new CSVEntry(parts[0], parts[1], Double.parseDouble(parts[3])));
        }
        return csvEntries;
    }
}
