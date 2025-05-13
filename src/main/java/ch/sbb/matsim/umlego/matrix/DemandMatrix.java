package ch.sbb.matsim.umlego.matrix;

import ch.sbb.matsim.umlego.readers.jdbc.TimesliceRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ch.sbb.matsim.umlego.matrix.MatrixUtil.minutesToMatrixName;

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


    public void setData(List<TimesliceRepository.TimesliceJdbcEntry> entries, ZonesLookup zonesLookup, boolean ignoreExcessZones) throws ZoneNotFoundException {
        Set<String> invalidZoneIds = new HashSet<>();
        for (TimesliceRepository.TimesliceJdbcEntry entry : entries) {
            int fromIndex = zonesLookup.getIndex(entry.from(), invalidZoneIds, ignoreExcessZones);;
            int toIndex = -1;
            try {
                fromIndex = zonesLookup.getIndex(entry.from());
            } catch (ZoneNotFoundException e) {
                if (!ignoreExcessZones) {
                    throw e;
                }
                invalidZoneIds.add(entry.from());
            }
            try {
                toIndex = zonesLookup.getIndex(entry.to());
            } catch (ZoneNotFoundException e) {
                if (!ignoreExcessZones) {
                    throw e;
                }
                invalidZoneIds.add(entry.to());
            }
            if (fromIndex >= 0 && toIndex >= 0) {
                getData()[fromIndex][toIndex] = entry.value();
            }
        }

        if (!invalidZoneIds.isEmpty()) {
            LOG.warn("The following Zone IDs weren't expected and are ignored: {}", invalidZoneIds);
        }
    }
}
