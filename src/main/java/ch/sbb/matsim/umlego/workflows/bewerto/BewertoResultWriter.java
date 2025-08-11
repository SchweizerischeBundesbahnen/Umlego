package ch.sbb.matsim.umlego.workflows.bewerto;

import ch.sbb.matsim.umlego.WorkResultHandler;
import ch.sbb.matsim.umlego.config.CompressionType;
import ch.sbb.matsim.umlego.matrix.Zones;
import ch.sbb.matsim.umlego.skims.UmlegoSkimCalculator;
import ch.sbb.matsim.umlego.writers.ResultWriter;
import com.opencsv.CSVWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Map;

/**
 * This handler writes Bewerto specific results.
 */
public class BewertoResultWriter implements WorkResultHandler<BewertoWorkResult> {

    private final CSVWriter writer;
    private Zones zones;

    public BewertoResultWriter(String outputFolder, CompressionType compressionType, Zones zones) {

        String output = ResultWriter.getFilename(outputFolder, "factors.csv", compressionType);
        this.zones = zones;

        try {
            writer = new CSVWriter(ResultWriter.newBufferedWriter(output), ';', CSVWriter.NO_QUOTE_CHARACTER,
                CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Write header row
        writer.writeNext(new String[]{"From_No", "To_No", "From_Name", "To_Name", "NTR_REF", "JRT_REF", "ADT_REF", "NTR_VAR", "JRT_VAR", "ADR_VAR", "F_JRT", "F_ADT", "F_NTR", "TotalFactor"});
    }

    @Override
    public void handleResult(BewertoWorkResult result) {

        for (Map.Entry<String, double[]> e : result.factors().entrySet()) {
            double[] factorValues = e.getValue();
            double[] skimsRef = result.skimsRef().get(e.getKey());
            double[] skimsVar = result.skimsVar().get(e.getKey());

            // Calculate total factors as the product of individual factors
            double totalFactor = factorValues[0] * factorValues[1] * factorValues[2];
            String[] row = {
                result.originZone(),
                e.getKey(),
                this.zones.getZone(result.originZone()).getName(),
                this.zones.getZone(e.getKey()).getName(),
                String.format(Locale.US, "%.6f", skimsRef[UmlegoSkimCalculator.NTR_IDX]),
                String.format(Locale.US, "%.6f", skimsRef[UmlegoSkimCalculator.JRT_IDX]),
                String.format(Locale.US, "%.6f", skimsRef[UmlegoSkimCalculator.ADT_IDX]),

                String.format(Locale.US, "%.6f", skimsVar[UmlegoSkimCalculator.NTR_IDX]),
                String.format(Locale.US, "%.6f", skimsVar[UmlegoSkimCalculator.JRT_IDX]),
                String.format(Locale.US, "%.6f", skimsVar[UmlegoSkimCalculator.ADT_IDX]),

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
