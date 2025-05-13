package ch.sbb.matsim.umlego.matrix;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MatrixUtil {

    private static final Logger LOG = LogManager.getLogger(MatrixUtil.class);

    /**
     * The default length of a time slice in minutes.
     * TODO: make this configurable.
     */
    public static final int TIME_SLICE_MIN = 10;

    private MatrixUtil() {
    }

    public static double[][] createData(int size, double defaultValue) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Allocating {} x {} = {} {} of memory", size, size, size * size / 1000 / 1000, "MB");
        }

        double[][] matrix = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                matrix[i][j] = defaultValue;
            }
        }
        return matrix;
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
