package ch.sbb.matsim.umlego.matrix;


import java.util.*;
import java.util.stream.Collectors;

import static ch.sbb.matsim.umlego.matrix.MatrixUtil.matrixNameToMinutes;
import static ch.sbb.matsim.umlego.matrix.MatrixUtil.minutesToMatrixIndex;

/**
 * Represents a collection of DemandMatrix objects with associated operations.
 * Allows retrieval, validation, and manipulation of DemandMatrix objects based on time indices.
 */
public class DemandMatrices {

    private ZonesLookup zonalLookup;
    private final Map<Integer, DemandMatrix> matrices;

    private final static DemandMatrices INSTANCE = new DemandMatrices();

    public DemandMatrices(ZonesLookup zonesLookup) {
        this.zonalLookup = zonesLookup;
        this.matrices = new HashMap<>();
    }

    public static DemandMatrices getInstance() {
        return INSTANCE;
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

    public void setZonesLookup(ZonesLookup zonalLookup) {
        this.zonalLookup = zonalLookup;
    }

    public Map<Integer, DemandMatrix> getMatrices() {
        return matrices;
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
        assert isSorted;
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

    /**
     * @return the sum of all the values in all the matrices
     */
    public double getSum() {
        return this.matrices.values().stream().mapToDouble(AbstractMatrix::getSum).sum();
    }

    /**
     * @return the average of all the values in all the matrices
     */
    public double getAverage() {
        return this.getSum() / (this.matrices.size() * Math.pow(this.zonalLookup.getAllLookupValues().size(), 2));
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
