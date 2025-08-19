package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.matrix.FactorMatrix;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.Zones;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvMultiMatrixParser extends AbstractCsvMatrixParser implements MatricesParser {

    private final String separator;
    private final FactorMatrix baseDemand;

    public CsvMultiMatrixParser(String path, Zones zones, double defaultValue, String separator, FactorMatrix baseDemand) {
        super(path, zones, defaultValue);
        this.separator = separator;
        this.baseDemand = baseDemand;
    }

    public CsvMultiMatrixParser(String path, Zones zones, double defaultValue, String separator) {
        this(path, zones, defaultValue, separator, null);
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
    public Matrices parse() throws ZoneNotFoundException {
        try {
            Map<Integer, List<CSVEntry>> csvEntries = parseCsvMultiMatrix(getPath(), this.separator);
            Matrices matrices = csvEntriesToDemandMatrices(csvEntries);
            if (baseDemand != null) {
                matrices.multiplyWith(baseDemand);
            }
            return matrices;
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
