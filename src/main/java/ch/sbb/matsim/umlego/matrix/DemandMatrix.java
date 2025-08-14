package ch.sbb.matsim.umlego.matrix;

import static ch.sbb.matsim.umlego.matrix.MatrixUtil.minutesToMatrixName;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DemandMatrix extends AbstractMatrix {

    private static final Logger LOG = LogManager.getLogger(DemandMatrix.class);

    private final TimeWindow timeWindow;

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
        this.timeWindow = new TimeWindow(startTimeInclusiveMin, endTimeExclusiveMin);
    }

    /**
     * Constructs a DemandMatrix object by copying the values from another
     */
    public DemandMatrix(DemandMatrix value) {
        super(value);
        this.timeWindow = new TimeWindow(value.timeWindow.startTimeInclusiveMin(), value.timeWindow.endTimeExclusiveMin());
    }

    public TimeWindow getTimeWindow() {
        return timeWindow;
    }

}
