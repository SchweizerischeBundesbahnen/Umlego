package ch.sbb.matsim.umlego.matrix;

import ch.sbb.matsim.umlego.readers.jdbc.TimesliceRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MatrixUtil {

    private static final Logger LOG = LogManager.getLogger(MatrixUtil.class);

    /**
     * The default length of a time slice in minutes.
     * TODO: make this configurable.
     */
    public static final int TIME_SLICE_MIN = 10;

    private MatrixUtil() {}

    public static double[][] createData(int size, double defaultValue) {
        if (LOG.isDebugEnabled()){
            LOG.debug("Allocating {} x {} = {} {} of memory", size, size, size*size/1000/1000, "MB");
        }

        double[][] matrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = defaultValue;
            }
        }
        return matrix;
    }

    /**
     * Converts a list of JDBCEntry objects into a 2-dimensional array representing the matrix data. The matrix is initialized with a default value, and values are filled based on the given JDBC
     * entries. If a zone ID in the JDBC entries is not found, the behavior depends on the ignoreExcessZones parameter.
     *
     * @param timesliceJdbcEntries the list of JDBCEntry objects representing rows of origin-destination pairs with values
     * @param ignoreExcessZones if true, entries with zone IDs not found in the lookup are ignored; if false, a ZoneNotFoundException is thrown for such entries
     * @return a 2-dimensional array where each cell [i][j] contains the value for the matrix entry
     * @throws ZoneNotFoundException if a zone ID in the JDBC entries is not found and ignoreExcessZones is false from zone i to zone j
     */
    public static double[][] convertToDataArrayJdbc(ZonesLookup zonesLookup, List<TimesliceRepository.TimesliceJdbcEntry> timesliceJdbcEntries, boolean ignoreExcessZones, double defaultValue) throws ZoneNotFoundException {
        double[][] data = createData(zonesLookup.size(), defaultValue);

        Set<String> invalidZoneIds = new HashSet<>();
        for (TimesliceRepository.TimesliceJdbcEntry entry : timesliceJdbcEntries) {
            int fromIndex = zonesLookup.getIndex(entry.from(), invalidZoneIds, ignoreExcessZones);
            int toIndex = zonesLookup.getIndex(entry.to(), invalidZoneIds, ignoreExcessZones);
            if (fromIndex >= 0 && toIndex >= 0) {
                data[fromIndex][toIndex] = entry.value();
            }
        }

        if (!invalidZoneIds.isEmpty()) {
            LOG.warn("The following Zone IDs weren't expected and are ignored: {}", invalidZoneIds);
        }

        return data;
    }




    // Helper methods to convert DemandMatrix names to and from minutes
    public static int matrixNameToMinutes(String name) {
        return matrixIndexToMinutes(Integer.parseInt(name) - 1);
    }

    public static int matrixIndexToMinutes(int index) {
        return index * TIME_SLICE_MIN;
    }

    public static int minutesToMatrixIndex(int minutes) {
        return minutes / TIME_SLICE_MIN;
    }

    public static String minutesToMatrixName(int minutes) {
        return String.valueOf(minutesToMatrixIndex(minutes) + 1);
    }
}
