package ch.sbb.matsim.umlego.matrix;

import it.unimi.dsi.fastutil.longs.LongLongPair;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a collection of DemandMatrix objects with associated operations. Allows retrieval, validation, and manipulation of DemandMatrix objects based on time indices.
 */
public class Matrices {

    private static final Logger LOG = LogManager.getLogger(Matrices.class);

    @Getter private ZonesLookup zonesLookup;
    @Getter private Zones zones;
    private final Map<TimeWindow, DemandMatrix> demandMatricesByTimewindow;
    private final Map<String, ShareMatrix> shareMatricesBySegment;

    public Matrices(List<AbstractMatrix> matrices, Zones zones, ZonesLookup zonesLookup) {
        this.zones = zones;
        this.zonesLookup = zonesLookup;
        this.demandMatricesByTimewindow = matrices.stream().filter(m -> m instanceof DemandMatrix).map(m -> (DemandMatrix) m).collect(Collectors.toMap(DemandMatrix::getTimeWindow, m -> m));
        this.shareMatricesBySegment = matrices.stream().filter(m -> m instanceof ShareMatrix).map(m -> (ShareMatrix) m).collect(Collectors.toMap(ShareMatrix::getSegment, m -> m));

        LOG.info("Loaded {} demand matrices and {} share matrices for {} zones.",
            this.demandMatricesByTimewindow.size(), this.shareMatricesBySegment.size(), this.zones.getAllZoneNos().size());

        this.validateShareMatrices();

        LOG.info("- Demand Total: {}", this.getSum());
        LOG.info("- Segment Shares: {}", this.getSegments());

    }

    private void validateShareMatrices() {
        if (this.shareMatricesBySegment.isEmpty()) {
            LOG.error("No share matrices found. This may lead to incorrect results.");
        } else {

            var m = this.shareMatricesBySegment.values().stream().findFirst().orElseThrow(() -> new IllegalStateException("No share matrices found"));
            double[][] data = m.getData();
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {

                    if (i == j) {
                        continue;
                    }

                    int finalI = i;
                    int finalJ = j;
                    var sum = this.shareMatricesBySegment.values().stream().map(x -> x.getData()[finalI][finalJ]).reduce(0.0, Double::sum);
                    if (sum > 0.001 && (sum < 0.999 || sum > 1.001)) {
                        LOG.error("Share matrix value at ({}, {}) is {}, expected to be close to 1.0", i, j, sum);
                        throw new UnsupportedOperationException("Share matrix value not summing to 1.0");
                    }

                }

            }

        }
    }

    public List<DemandMatrix> getDemandMatrices() {
        return this.demandMatricesByTimewindow.values().stream()
            .sorted(Comparator.comparingInt(m -> m.getTimeWindow().startTimeInclusiveMin()))
            .toList();
    }

    public List<String> getSegments() {
        return this.shareMatricesBySegment.keySet().stream().sorted().toList();
    }

    public List<TimeWindow> getTimeWindows() {
        return this.demandMatricesByTimewindow.values().stream()
            .map(DemandMatrix::getTimeWindow)
            .sorted(Comparator.comparingInt(TimeWindow::startTimeInclusiveMin))
            .toList();
    }

    public DemandMatrix getDemandMatrix(TimeWindow timeWindow) {
        return this.demandMatricesByTimewindow.get(timeWindow);
    }

    /**
     * Multiplies all Matrices element-wise with the provided Matrix.
     *
     * @param matrix the Matrix to multiply with
     */
    public void multiplyWith(Matrix matrix) {
        this.demandMatricesByTimewindow.values().forEach((v) -> v.multiplyWith(matrix));
    }

    public double getMatrixValue(String fromZoneNo, String toZoneNo, TimeWindow timeWindow) throws ZoneNotFoundException {
        Matrix matrix = getDemandMatrix(timeWindow);
        int fromIndex = this.zonesLookup.getIndex(fromZoneNo);
        int toIndex = this.zonesLookup.getIndex(toZoneNo);
        return matrix.getValue(fromIndex, toIndex);
    }

    public double getShareMatrixValue(String segment, String fromZoneNo, String toZoneNo) throws ZoneNotFoundException {
        Matrix matrix = this.shareMatricesBySegment.get(segment);
        int fromIndex = this.zonesLookup.getIndex(fromZoneNo);
        int toIndex = this.zonesLookup.getIndex(toZoneNo);
        return matrix.getValue(fromIndex, toIndex);
    }

    /**
     * @return the sum of all the values in all the matrices
     */
    public double getSum() {
        return this.demandMatricesByTimewindow.values().parallelStream().mapToDouble(AbstractMatrix::getSum).sum();
    }

    /**
     * @return the average of all the values in all the matrices
     */
    public double getAverage() {
        return this.getSum() / (this.demandMatricesByTimewindow.size() * Math.pow(this.zones.getAllZoneNos().size(), 2));
    }

    /**
     * Percentage of non-zero elements in all matrices.
     */
    public double getLoadFactor() {

        Optional<LongLongPair> result = this.demandMatricesByTimewindow.values().parallelStream().map(m -> {
            double[][] data = m.getData();
            long nonZeroCount = 0;
            long total = 0;
            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    if (data[i][j] != 0) {
                        nonZeroCount++;
                    }
                }

                total += data[i].length;
            }
            return LongLongPair.of(total, nonZeroCount);
        }).reduce((a, b) -> LongLongPair.of(a.leftLong() + b.leftLong(), a.rightLong() + b.rightLong()));

        return result.map(longLongPair -> longLongPair.rightLong() / (double) longLongPair.leftLong()).orElse(Double.NaN);

    }

    /**
     * @return a list of matrix names ordered by start time
     */
    public List<String> getMatrixNames() {
        return this.demandMatricesByTimewindow.values().stream().map(AbstractMatrix::getName).toList();
    }

    public double getOriginSum(String zoneNo) throws ZoneNotFoundException {
        int originIndex = zonesLookup.getIndex(zoneNo);
        return demandMatricesByTimewindow.values().stream()
            .mapToDouble(matrix -> matrix.getOriginSum(originIndex))
            .sum();
    }

}
