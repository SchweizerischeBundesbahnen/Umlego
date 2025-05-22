package ch.sbb.matsim.umlego.writers;

import ch.sbb.matsim.umlego.Umlego;
import ch.sbb.matsim.umlego.UmlegoSkimCalculator;
import ch.sbb.matsim.umlego.writers.types.skim.ODPair;
import com.opencsv.CSVWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

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

        for (var skimType : this.skims.getCalculators()) {
            headers.add(skimType.getSkimType().toString());
        }
        return headers.toArray(new String[0]);
    }

    @Override
    public void close() throws Exception {

        LOG.info("Writing skim matrices...");

        try (var writer = new CSVWriter(UmlegoWriter.newBufferedWriter(filename), ',', '"', '\\', "\n")) {

            writer.writeNext(createHeaderRow());

            for (ODPair odPair : skims.getSkims().keySet()) {
                var row = new ArrayList<String>();
                row.add(odPair.fromZone());
                row.add(odPair.toZone());
                var matrices = this.skims.getSkims().get(odPair);
                for (var skimType : this.skims.getCalculators()) {
                    row.add(String.format("%.5f", matrices.getDouble(skimType.getSkimType())));
                }
                writer.writeNext(row.toArray(String[]::new));
            }
        }
        LOG.info("Done with skim matrices.");
    }
}
