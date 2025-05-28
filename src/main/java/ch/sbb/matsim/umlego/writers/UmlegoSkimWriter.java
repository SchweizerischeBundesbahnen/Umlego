package ch.sbb.matsim.umlego.writers;

import ch.sbb.matsim.umlego.Umlego;
import ch.sbb.matsim.umlego.UmlegoResultWorker;
import ch.sbb.matsim.umlego.UmlegoSkimCalculator;
import ch.sbb.matsim.umlego.writers.types.skim.ODPair;
import ch.sbb.matsim.umlego.writers.types.skim.SkimCalculator;
import com.opencsv.CSVWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes skim matrices to a CSV file.
 */
public final class UmlegoSkimWriter implements UmlegoWriter {

    private static final Logger LOG = LogManager.getLogger(UmlegoSkimWriter.class);

    private final UmlegoSkimCalculator skims;
    private final CSVWriter writer;

    public UmlegoSkimWriter(UmlegoSkimCalculator skims, String filename){
        this.skims = skims;

        try {
            this.writer = new CSVWriter(UmlegoResultWorker.newBufferedWriter(filename), ',', '"', '\\', "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        };

        writer.writeNext(createHeaderRow());
    }

    private String[] createHeaderRow() {
        List<String> headers = new ArrayList<>();
        headers.add("ORIGIN");
        headers.add("DESTINATION");

        for (SkimCalculator skimType : this.skims.getCalculators()) {
            headers.add(skimType.getSkimType().toString());
        }
        return headers.toArray(new String[0]);
    }

    @Override
    public void writeRoute(String origZone, String destZone, Umlego.FoundRoute route) {
        // Nothing needs to be done here
    }

    @Override
    public void writeODPair(String origZone, String destZone) {

        String[] row = new String[this.skims.getCalculators().size() + 2];

        row[0] = origZone;
        row[1] = destZone;
        double[] values = skims.getSkims().get(new ODPair(origZone, destZone));

        for (int i = 0; i < values.length; i++) {
            row[i + 2] = String.format(Locale.US, "%.5f", values[i]);
        }

        writer.writeNext(row);
    }

    @Override
    public void close() throws Exception {

        writer.close();

        LOG.info("Done with skim matrices.");
    }

}
