package ch.sbb.matsim.umlego.demand;

import ch.sbb.matsim.umlego.demand.csv.CsvUnroutableDemandWriter;
import ch.sbb.matsim.umlego.demand.jdbc.JdbcUnroutableDemandWriter;
import java.time.LocalDate;

public final class UnroutableDemandWriterFactory {

    private UnroutableDemandWriterFactory() {}

    public static UnroutableDemandWriter createWriter(String outputFolder, String runId, LocalDate targetDate) {
        if (runId == null) {
            return new CsvUnroutableDemandWriter(outputFolder);
        } else {
            return new JdbcUnroutableDemandWriter(runId, targetDate);
        }
    }
}
