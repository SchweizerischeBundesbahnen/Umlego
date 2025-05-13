package ch.sbb.matsim.umlego.matrix;

import static ch.sbb.matsim.umlego.matrix.MatrixUtil.minutesToMatrixName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DemandMatrix extends AbstractMatrix {

    private static final Logger LOG = LogManager.getLogger(DemandMatrix.class);

    private int startTimeInclusiveMin;
    private int endTimeExclusiveMin;

    /**
     * Constructs a DemandMatrix object for the specified start and end times.
     *
     * @param startTimeInclusiveMin the start time in minutes (inclusive)
     * @param endTimeExclusiveMin the end time in minutes (exclusive)
     * @param data the data array representing the matrix
     */
    public DemandMatrix(int startTimeInclusiveMin, int endTimeExclusiveMin, double[][] data) {
        super(data, minutesToMatrixName(startTimeInclusiveMin));
        assert startTimeInclusiveMin < endTimeExclusiveMin;
        this.startTimeInclusiveMin = startTimeInclusiveMin;
        this.endTimeExclusiveMin = endTimeExclusiveMin;
    }

    /**
     * @return start time in minutes, inclusive, for which this matrix is defined
     */
    public int getStartTimeInclusiveMin() {
        return startTimeInclusiveMin;
    }

    /**
     * @return end time in minutes, exclusive, for which this matrix is defined
     */
    public int getEndTimeExclusiveMin() {
        return endTimeExclusiveMin;
    }

}
