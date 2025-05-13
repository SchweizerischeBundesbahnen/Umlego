package ch.sbb.matsim.umlego.matrix;

public interface Matrix {

    /**
     * @return a 2-dimensional array of the matrix's data values
     */
    double[][] getData();

    /**
     * Multiplies the current matrix element-wise with the provided matrix.
     *
     * @param matrix the matrix to multiply with
     */
    void multiplyWith(Matrix matrix);

    String getName();

    /**
     * Returns the value of the matrix at the given indices.
     *
     * @param fromIndex the row index
     * @param toIndex the column index
     * @return the value at the given indices
     */
    double getValue(int fromIndex, int toIndex);

    /**
     * @return the sum of all matrix elements
     */
    double getSum();
    /**
     * @return the average value of all matrix elements
     */
    double getAverage();
    /**
     * @return the minimum of all matrix elements
     */
    double getMin();
    /**
     * @return the maximum value of all matrix elements
     */
    double getMax();

    /**
     * Resets whole matrix with defaultValue.
     * @param defaultValue value for all rows and columns
     */
    void reset(double defaultValue);

    /**
     * Get the row sum of the matrix, i.e. the sum over all destinations for a given origin.
     * @param originIndex row index
     */
    double getOriginSum(int originIndex);

}
