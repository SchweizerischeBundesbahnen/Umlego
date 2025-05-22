package ch.sbb.matsim.umlego.writers;

import ch.sbb.matsim.umlego.Umlego;
import ch.sbb.matsim.umlego.UmlegoSkimCalculator;
import ch.sbb.matsim.umlego.writers.types.skim.ODPair;
import ch.sbb.matsim.umlego.writers.types.skim.SkimCalculator;
import com.opencsv.CSVWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Writes skim matrices to a CSV file.
 */
public final class UmlegoSkimWriter implements UmlegoWriterInterface {

    private static final Logger LOG = LogManager.getLogger(UmlegoSkimWriter.class);

    private final UmlegoSkimCalculator skims;
    private final String filename;

    public UmlegoSkimWriter(UmlegoSkimCalculator skims, String filename) {
        this.skims = skims;
        this.filename = filename;
    }

    @Override
    public void writeRoute(String origZone, String destZone, Umlego.FoundRoute route) {

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
    public void close() throws Exception {

        LOG.info("Writing skim matrices...");

        try (var writer = new CSVWriter(UmlegoWriter.newBufferedWriter(filename), ',', '"', '\\', "\n")) {

            writer.writeNext(createHeaderRow());

            String[] row = new String[this.skims.getCalculators().size() + 2];

            for (ODPair odPair : skims.getSkims().keySet()) {

                row[0] = odPair.fromZone();
                row[1] = odPair.toZone();

                double[] matrices = this.skims.getSkims().get(odPair);

                for (int i = 0; i < matrices.length; i++) {
                    row[i + 2] = String.format(Locale.US, "%.5f", matrices[i]);
                }

                writer.writeNext(row);
            }
        }
        LOG.info("Done with skim matrices.");
    }
}
