package ch.sbb.matsim.umlego.matrix;

import java.util.Arrays;
import java.util.stream.DoubleStream;

public abstract class AbstractMatrix implements Matrix {

    private final String name;

    private final double[][] data;

    public AbstractMatrix(double[][] data, String name) {
        this.data = data;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public double[][] getData() {
        return data;
    }

    @Override
    public void reset(double defaultValue) {
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data.length; j++) {
                data[i][j] = defaultValue;
            }
        }
    }

    @Override
    public double getValue(int fromIndex, int toIndex) {
        return data[fromIndex][toIndex];
    }

    @Override
    public void multiplyWith(Matrix matrix) {

        int rowsFirst = this.getData().length;
        int columnsFirst = this.getData()[0].length;
        int rowsSecond = matrix.getData().length;
        int columnsSecond = matrix.getData()[0].length;

        if (rowsFirst != rowsSecond || columnsFirst != columnsSecond) {
            throw new IllegalArgumentException("Matrices dimensions do not match for element-wise multiplication.");
        }
        for (int i = 0; i < rowsFirst; i++) {
            for (int j = 0; j < columnsFirst; j++) {
                this.getData()[i][j] = this.getData()[i][j] * matrix.getData()[i][j];
            }
        }
    }

    @Override
    public double getSum() {
        return Arrays.stream(this.data).flatMapToDouble(DoubleStream::of).sum();
    }

    @Override
    public double getAverage() {
        return Arrays.stream(this.data).flatMapToDouble(DoubleStream::of).average().orElse(Double.NaN);
    }

    @Override
    public double getMin() {
        return Arrays.stream(this.data).flatMapToDouble(DoubleStream::of).min().orElse(Double.NaN);
    }

    @Override
    public double getMax() {
        return Arrays.stream(this.data).flatMapToDouble(DoubleStream::of).max().orElse(Double.NaN);
    }

    @Override
    public double getOriginSum(int originIndex) {
        return Arrays.stream(this.data[originIndex]).sum();
    }

}
