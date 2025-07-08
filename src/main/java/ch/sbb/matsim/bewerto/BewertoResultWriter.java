package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.umlego.WorkResultHandler;
import ch.sbb.matsim.umlego.writers.ResultWriter;
import com.opencsv.CSVWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;

/**
 * This handler writes Bewerto specific results.
 */
public class BewertoResultWriter implements WorkResultHandler<BewertoWorkResult> {

    private final CSVWriter writer;

    public BewertoResultWriter(String outputFolder) {

        String output = Paths.get(outputFolder, "factors.csv.gz").toString();
        try {
            writer = new CSVWriter(ResultWriter.newBufferedWriter(output), ';', CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Write header row
        writer.writeNext(new String[]{"From", "To", "F_JRT", "F_ADT", "F_NTR", "TotalFactor"});
    }

    @Override
    public void handleResult(BewertoWorkResult result) {

        for (Map.Entry<String, double[]> e : result.factors().entrySet()) {
            double[] factorValues = e.getValue();

            // Calculate total factors as the product of individual factors
            double totalFactor = factorValues[0] * factorValues[1] * factorValues[2];
            String[] row = {
                result.originZone(),
                e.getKey(),
                String.format(Locale.US, "%.6f", factorValues[0]),
                String.format(Locale.US, "%.6f", factorValues[1]),
                String.format(Locale.US, "%.6f", factorValues[2]),
                String.format(Locale.US, "%.6f", totalFactor)
            };

            writer.writeNext(row);
        }
    }

    @Override
    public void close() throws Exception {
        writer.close();
    }
}
