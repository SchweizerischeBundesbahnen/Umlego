package ch.sbb.matsim.umlego.matrix;


import it.unimi.dsi.fastutil.longs.LongLongPair;

import java.util.*;
import java.util.stream.Collectors;

import static ch.sbb.matsim.umlego.matrix.MatrixUtil.*;

/**
 * Represents a collection of DemandMatrix objects with associated operations.
 * Allows retrieval, validation, and manipulation of DemandMatrix objects based on time indices.
 */
public class DemandMatrices {

    private ZonesLookup zonalLookup;
    private final Map<Integer, DemandMatrix> matrices;

    public DemandMatrices(ZonesLookup zonesLookup) {
        this.zonalLookup = zonesLookup;
        this.matrices = new HashMap<>();
    }

    public DemandMatrices() {
        this.zonalLookup = new ZonesLookup(new HashMap<>());
        this.matrices = new HashMap<>();
    }

    public DemandMatrices(List<DemandMatrix> matrices, ZonesLookup zonalLookup) {
        this.zonalLookup = zonalLookup;
        validateMatrices(matrices);
        this.matrices = matrices.stream().collect(Collectors.toMap(m -> minutesToMatrixIndex(m.getStartTimeInclusiveMin()), m -> m));
    }

    /**
     * Copy constructor for the demand, creates copies of the matrices.
     */
    public DemandMatrices(DemandMatrices demand) {
        this.zonalLookup = demand.zonalLookup;
        this.matrices = new HashMap<>();
        for (Map.Entry<Integer, DemandMatrix> entry : demand.matrices.entrySet()) {
            this.matrices.put(entry.getKey(), new DemandMatrix(entry.getValue()));
        }
    }

    public void setZonesLookup(ZonesLookup zonalLookup) {
        this.zonalLookup = zonalLookup;
    }

    public Map<Integer, DemandMatrix> getMatrices() {
        return matrices;
    }

    public Collection<String> getZoneIds() {
        return this.zonalLookup.getAllLookupValues();
    }

    /**
     * Gets the DemandMatrix associated with the given time index (in minutes).
     * @param timeMin the time index in minutes
     * @return the DemandMatrix associated with the given time index
     */
    public DemandMatrix getMatrix(int timeMin) {
        return this.matrices.get(minutesToMatrixIndex(timeMin));
    }

    /**
     * Multiplies all Matrices element-wise with the provided Matrix.
     * @param matrix the Matrix to multiply with
     */
    public void multiplyWith(Matrix matrix) {
        this.matrices.forEach((k, v) -> v.multiplyWith(matrix));
    }

    /**
     * Checks the given list of DemandMatrices for consistency.
     * Namely, that all matrices have the same time range (i.e., same number of minutes)
     * and that there is no overlap between them.
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

    public ZonesLookup getLookup() {
        return this.zonalLookup;
    }

    public double getMatrixValue(String fromZoneId, String toZoneId, int timeMin) throws ZoneNotFoundException {
        Matrix matrix = getMatrix(timeMin);
        int fromIndex = this.zonalLookup.getIndex(fromZoneId);
        int toIndex = this.zonalLookup.getIndex(toZoneId);
        return matrix.getValue(fromIndex, toIndex);

    }

    public double getMatrixValue(String fromZoneId, String toZoneId, String name) throws ZoneNotFoundException {
        return getMatrixValue(fromZoneId, toZoneId, matrixNameToMinutes(name));
    }

    public void multiplyMatrixValues(DemandMatrixMultiplier multiplier) {
        for (Map.Entry<Integer, DemandMatrix> entry : this.matrices.entrySet()) {

            int timeMin = matrixIndexToMinutes(entry.getKey());
            DemandMatrix matrix = entry.getValue();

            double[][] data = matrix.getData();

            for (int i = 0; i < data.length; i++) {
                String fromZone = zonalLookup.getZone(i);
                for (int j = 0; j < data[i].length; j++) {
                    String toZone = zonalLookup.getZone(j);

                    double value = data[i][j];
                    if (value != 0) {
                        double f = multiplier.getFactor(fromZone, toZone, timeMin);
                        data[i][j] = value * f;
                    }
                }
            }
        }
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
        return this.getSum() / (this.matrices.size() * Math.pow(this.zonalLookup.getAllLookupValues().size(), 2));
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

    public double getOriginSum(String zoneId) throws ZoneNotFoundException {
        int originIndex = zonalLookup.getIndex(zoneId);
        return matrices.values().stream()
            .mapToDouble(matrix -> matrix.getOriginSum(originIndex))
            .sum();
    }

}
