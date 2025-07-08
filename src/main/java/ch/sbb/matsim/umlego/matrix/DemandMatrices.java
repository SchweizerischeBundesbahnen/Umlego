package ch.sbb.matsim.umlego.matrix;

import static ch.sbb.matsim.umlego.matrix.MatrixUtil.matrixNameToMinutes;
import static ch.sbb.matsim.umlego.matrix.MatrixUtil.minutesToMatrixIndex;

import it.unimi.dsi.fastutil.longs.LongLongPair;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a collection of DemandMatrix objects with associated operations. Allows retrieval, validation, and manipulation of DemandMatrix objects based on time indices.
 */
public class DemandMatrices {

    private ZonesLookup zonesLookup;
    private Zones zones;
    private final Map<Integer, DemandMatrix> matrices;

    public DemandMatrices(List<DemandMatrix> matrices, Zones zones, ZonesLookup zonesLookup) {
        this.zones = zones;
        this.zonesLookup = zonesLookup;
        validateMatrices(matrices);
        this.matrices = matrices.stream().collect(Collectors.toMap(m -> minutesToMatrixIndex(m.getStartTimeInclusiveMin()), m -> m));
    }

    public Map<Integer, DemandMatrix> getMatrices() {
        return matrices;
    }

    /**
     * Gets the DemandMatrix associated with the given time index (in minutes).
     *
     * @param timeMin the time index in minutes
     * @return the DemandMatrix associated with the given time index
     */
    public DemandMatrix getMatrix(int timeMin) {
        return this.matrices.get(minutesToMatrixIndex(timeMin));
    }

    /**
     * Multiplies all Matrices element-wise with the provided Matrix.
     *
     * @param matrix the Matrix to multiply with
     */
    public void multiplyWith(Matrix matrix) {
        this.matrices.forEach((k, v) -> v.multiplyWith(matrix));
    }

    /**
     * Checks the given list of DemandMatrices for consistency. Namely, that all matrices have the same time range (i.e., same number of minutes) and that there is no overlap between them.
     *
     * @param matrices the list of DemandMatrices to validate
     */
    private void validateMatrices(List<DemandMatrix> matrices) {
        // all start-end deltas are equal
        assert 1 == matrices.stream().map(m -> m.getEndTimeExclusiveMin() - m.getStartTimeInclusiveMin()).collect(Collectors.toSet()).size();
        // there is no time overlap among matrices
        List<Integer> allMinutes = new ArrayList<>();
        matrices.forEach(m -> {
            for (int i = m.getStartTimeInclusiveMin(); i < m.getEndTimeExclusiveMin(); i++) {
                allMinutes.add(i);
            }
        });
        assert allMinutes.size() == new HashSet<>(allMinutes).size();
        // times are in proper order
        boolean isSorted = matrices.stream()
            .sorted(Comparator.comparingInt(DemandMatrix::getStartTimeInclusiveMin))
            .toList()
            .equals(matrices);

        assert isSorted : "Demand matrices are not sorted by start time";
    }

    private int compareStartTime(DemandMatrix m1, DemandMatrix m2) {
        return m1.getStartTimeInclusiveMin() - m2.getStartTimeInclusiveMin();
    }

    public Zones getLookup() {
        return this.zones;
    }

    public ZonesLookup getZonesLookup() {
        return this.zonesLookup;
    }

    public double getMatrixValue(String fromZoneNo, String toZoneNo, int timeMin) throws ZoneNotFoundException {
        Matrix matrix = getMatrix(timeMin);
        int fromIndex = this.zonesLookup.getIndex(fromZoneNo);
        int toIndex = this.zonesLookup.getIndex(toZoneNo);
        return matrix.getValue(fromIndex, toIndex);

    }

    public double getMatrixValue(String fromZoneId, String toZoneId, String name) throws ZoneNotFoundException {
        return getMatrixValue(fromZoneId, toZoneId, matrixNameToMinutes(name));
    }

    /**
     * @return the sum of all the values in all the matrices
     */
    public double getSum() {
        return this.matrices.values().parallelStream().mapToDouble(AbstractMatrix::getSum).sum();
    }

    /**
     * @return the average of all the values in all the matrices
     */
    public double getAverage() {
        return this.getSum() / (this.matrices.size() * Math.pow(this.zones.getAllNos().size(), 2));
    }

    /**
     * Percentage of non-zero elements in all matrices.
     */
    public double getLoadFactor() {

        Optional<LongLongPair> result = this.matrices.values().parallelStream().map(m -> {
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
        return this.matrices.values().stream().map(AbstractMatrix::getName).toList();
    }

    public double getOriginSum(String zoneNo) throws ZoneNotFoundException {
        int originIndex = zonesLookup.getIndex(zoneNo);
        return matrices.values().stream()
            .mapToDouble(matrix -> matrix.getOriginSum(originIndex))
            .sum();
    }

}
