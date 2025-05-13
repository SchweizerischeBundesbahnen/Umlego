package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static ch.sbb.matsim.umlego.matrix.MatrixUtil.createData;

public abstract class AbstractCsvMatrixParser {

    private static final Logger LOG = LogManager.getLogger(AbstractCsvMatrixParser.class);

    private final String path;
    private final ZonesLookup zonesLookup;
    private final double defaultValue;

    public record CSVEntry(String from, String to, double value) {
    }

    protected AbstractCsvMatrixParser(String path, ZonesLookup zonesLookup, double defaultValue) {
        this.path = path;
        this.zonesLookup = zonesLookup;
        this.defaultValue = defaultValue;
    }

    private ZonesLookup getZonesLookup() {
        return zonesLookup;
    }

    private double getDefaultValue() {
        return this.defaultValue;
    }

    protected String getPath() {
        return this.path;
    }

    /**
     * Parses a CSV file representing a matrix of origin-destination pairs.
     * Each line of the file is expected to contain exactly three columns: the origin
     * zone name, the destination zone name, and the value of the matrix entry.
     * The separator is specified by the {@code separator} parameter.
     * The header line of the file is ignored.
     *
     * @param br the reader for the CSV file to parse
     * @param separator the separator used in the CSV file (e.g. {@code ","}, {@code ";"}, etc.)
     * @return a list of {@link CSVEntry} objects, one for each line in the file
     * @throws IOException if an error occurs while reading the file
     */
    protected List<CSVEntry> parseCsvMatrix(BufferedReader br, String separator) throws IOException {
        List<CSVEntry> csvEntries = new ArrayList<>();
        br.readLine(); // skip header
        String line;
        while ((line = br.readLine()) != null) {
            String[] parts = line.trim().split(separator);
            csvEntries.add(new CSVEntry(parts[0], parts[1], Double.parseDouble(parts[2])));
        }
        return csvEntries;
    }

    protected List<CSVEntry> parseCsvMatrix(String filePath, String separator) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        return parseCsvMatrix(br, separator);
    }

    /**
     * Converts a list of CSVEntry objects into a 2-dimensional array representing the matrix data.
     * The matrix is initialized with a default value, and values are filled based on the given CSV entries.
     * If a zone ID in the CSV entries is not found, the behavior depends on the ignoreExcessZones parameter.
     *
     * @param csvEntries the list of CSVEntry objects representing rows of origin-destination pairs with values
     * @param ignoreExcessZones if true, entries with zone IDs not found in the lookup are ignored;
     *                          if false, a ZoneNotFoundException is thrown for such entries
     * @return a 2-dimensional array where each cell [i][j] contains the value for the matrix entry
     *         from zone i to zone j
     * @throws ZoneNotFoundException if a zone ID in the CSV entries is not found and ignoreExcessZones is false
     */
    protected double[][] convertToDataArray(List<CSVEntry> csvEntries, boolean ignoreExcessZones) throws ZoneNotFoundException {
        double[][] data = createData(getZonesLookup().size(), getDefaultValue());

        Set<String> invalidZoneIds = new HashSet<>();
        for (CSVEntry csvRow : csvEntries) {
            int fromIndex = -1;
            int toIndex = -1;
            try {
                fromIndex = getZonesLookup().getIndex(csvRow.from());
            } catch (ZoneNotFoundException e) {
                if (!ignoreExcessZones) throw e;
                invalidZoneIds.add(csvRow.from());
            }
            try {
                toIndex = getZonesLookup().getIndex(csvRow.to());
            } catch (ZoneNotFoundException e) {
                if (!ignoreExcessZones) throw e;
                invalidZoneIds.add(csvRow.to());
            }
            if (fromIndex >= 0 && toIndex >= 0) {
                data[fromIndex][toIndex] = csvRow.value();
            }
        }
        if (!invalidZoneIds.isEmpty()) {
            LOG.warn("The following Zone IDs weren't expected and are ignored: {}", invalidZoneIds);
        }
        return data;
    }

    protected DemandMatrices csvEntriesToDemandMatrices(Map<Integer, List<CSVEntry>> csvEntries) throws ZoneNotFoundException {
        List<DemandMatrix> matrices = new ArrayList<>();
        for (Entry<Integer, List<CSVEntry>> entry : csvEntries.entrySet()) {
            double[][] data = convertToDataArray(entry.getValue(), true);
            int index = entry.getKey();
            int startTimeMin = MatrixUtil.matrixIndexToMinutes(index);
            matrices.add(new DemandMatrix(startTimeMin, startTimeMin + MatrixUtil.TIME_SLICE_MIN, data));
        }

        return new DemandMatrices(matrices, getZonesLookup());
    }
}
