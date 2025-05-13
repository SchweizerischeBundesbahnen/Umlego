package ch.sbb.matsim.umlego.demand;

import ch.sbb.matsim.umlego.demand.csv.CsvUnroutableDemandWriter;

public final class UnroutableDemandWriterFactory {

    private UnroutableDemandWriterFactory() {
    }

    public static UnroutableDemandWriter createWriter(String outputFolder) {
        return new CsvUnroutableDemandWriter(outputFolder);
    }
}
