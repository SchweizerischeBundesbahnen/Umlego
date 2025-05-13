package ch.sbb.matsim.umlego.demand.csv;

import ch.sbb.matsim.umlego.demand.UnroutableDemand;
import ch.sbb.matsim.umlego.demand.UnroutableDemandPart;
import ch.sbb.matsim.umlego.demand.UnroutableDemandWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CsvUnroutableDemandWriter implements UnroutableDemandWriter {

    private static final Logger LOG = LogManager.getLogger(CsvUnroutableDemandWriter.class);

    private static final String FILENAME = "unroutable_demand.csv";

    private String path;

    public CsvUnroutableDemandWriter(String path) {
        this.path = path;
    }

    @Override
    public void write(UnroutableDemand unroutableDemand) {
        LOG.warn("Unroutable demand");
        LOG.warn("=================");
        LOG.warn("From,To,Demand");

        try (BufferedWriter unroutableWriter = new BufferedWriter(new FileWriter(Paths.get(path, FILENAME).toFile()))) {
            unroutableWriter.write("from,to,demand" + System.lineSeparator());
            for (UnroutableDemandPart part : unroutableDemand.getParts()) {
                LOG.warn("{},{},{}", part.fromZone(), part.toZone(), part.demand());
                unroutableWriter.write(part.fromZone() + "," + part.toZone() + "," + part.demand() + System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOG.warn("-----------------");
        LOG.warn("Total {}", unroutableDemand.sum());
        LOG.warn("=================");

    }
}
