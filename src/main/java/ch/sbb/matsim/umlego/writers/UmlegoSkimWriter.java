package ch.sbb.matsim.umlego.writers;

import ch.sbb.matsim.umlego.UmlegoWorkResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.opencsv.CSVWriter;

import ch.sbb.matsim.umlego.FoundRoute;
import ch.sbb.matsim.umlego.UmlegoListener;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkResult;
import ch.sbb.matsim.umlego.config.WriterParameters;
import ch.sbb.matsim.umlego.skims.SkimCalculator;
import ch.sbb.matsim.umlego.skims.UmlegoSkimCalculator;
import static ch.sbb.matsim.umlego.writers.ResultWriter.newBufferedWriter;

/**
 * Writes skim matrices to a CSV file.
 */
public final class UmlegoSkimWriter implements UmlegoListener {

    private final CSVWriter writer;
    private final String[] row = new String[UmlegoSkimCalculator.INSTANCE.getCalculators().size() + 2];

    public UmlegoSkimWriter(String filename, WriterParameters params) {
        try {
            this.writer = new CSVWriter(newBufferedWriter(filename), ',', '"', '\\', "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        writer.writeNext(createHeaderRow());
    }

    private String[] createHeaderRow() {
        List<String> headers = new ArrayList<>();
        headers.add("ORIGIN");
        headers.add("DESTINATION");

        for (SkimCalculator skimType : UmlegoSkimCalculator.INSTANCE.getCalculators()) {
            headers.add(skimType.getSkimType().toString());
        }
        return headers.toArray(new String[0]);
    }

    @Override
    public void processRoute(String origZone, String destZone, FoundRoute route) {
        // Nothing needs to be done here
    }

    @Override
    public void processResult(WorkResult result, String destZone) {

        if (!(result instanceof UmlegoWorkResult wr)) {
            return;
        }

        row[0] = result.originZone();
        row[1] = destZone;

        double[] values = wr.skims().get(destZone);
        if (values == null) {
            return;
        }

        for (int i = 0; i < values.length; i++) {
            row[i + 2] = String.format(Locale.US, "%.5f", values[i]);
        }

        writer.writeNext(row);
    }

    @Override
    public void finish() throws Exception {
        writer.close();
    }

}
