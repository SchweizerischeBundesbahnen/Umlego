/*
 * Copyright (C) Schweizerische Bundesbahnen SBB, 2017.
 */

package ch.sbb.matsim.umlego.writers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedWriter;
import java.io.IOException;

import static ch.sbb.matsim.umlego.writers.ResultWriter.newBufferedWriter;

/**
 * The HeaderColumnWriter allows so write a header before the comma-separated colums.
 * Used i.e. for writing Visum files.
 */
final class VisumTabularFileWriter implements AutoCloseable {

    private static final Logger LOG = LogManager.getLogger(VisumTabularFileWriter.class);

    public static final String DEFAULT_SEPARATOR = ";";
    private final String separator;
    private final String[] columns;
    private final int columnCount;
    private final BufferedWriter writer;
    private final String[] currentRow;
    private int counter;

    VisumTabularFileWriter(final String header, final String[] columns, final String filename) throws IOException {
        this(header, columns, newBufferedWriter(filename), DEFAULT_SEPARATOR);
    }

    private VisumTabularFileWriter(final String header, final String[] columns, final BufferedWriter writer, final String separator) throws IOException {
        this.columns = columns;
        this.columnCount = this.columns.length;
        this.currentRow = new String[this.columnCount];
        this.writer = writer;
        this.separator = separator;

        // write header data
        if (header != null) {
            this.writer.write(header);
        }

        // write column names
        for (int i = 0; i < this.columnCount; i++) {
            if (i > 0) {
                this.writer.write(this.separator);
            }
            String col = columns[i];
            this.writer.write(col);
        }
        this.writer.write("\n");

        clearRow();
    }

    /**
     * Sets the column in the current row to the specified value;
     *
     * @param column sets column
     * @param value  with value
     */
    void set(String column, String value) {
        for (int i = 0; i < this.columnCount; i++) {
            if (this.columns[i].equals(column)) {
                this.currentRow[i] = value;
                return;
            }
        }
        throw new IllegalArgumentException("Column not found: " + column);
    }

    /**
     * Writes the current row to the file and clears the current row afterward.
     */
    void writeRow() {
        try {
            writeRow(false);
        } catch (IOException e) {
            LOG.error("writeRow", e);
        }
    }

    void writeRow(boolean flush) throws IOException {
        try {
            for (int i = 0; i < this.columnCount; i++) {
                if (i > 0) {
                    this.writer.write(this.separator);
                }
                this.writer.write(this.currentRow[i]);
            }
            this.writer.write("\n");
            if (flush) {
                this.writer.flush();
            }
        } catch (IOException e) {
            throw new IOException(e);
        }
        this.counter++;
        clearRow();
    }

    private void clearRow() {
        for (int i = 0; i < this.columns.length; i++) {
            this.currentRow[i] = "";
        }
    }

    @Override
    public void close() throws IOException {
        if (this.counter > 0) {
            LOG.info("Wrote {} rows to file.", this.counter);
        } else {
            LOG.warn("No rows written to file.");
        }
        this.writer.close();
    }

}
