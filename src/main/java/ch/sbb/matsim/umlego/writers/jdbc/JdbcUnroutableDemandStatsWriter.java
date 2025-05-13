package ch.sbb.matsim.umlego.writers.jdbc;

import static ch.sbb.matsim.umlego.UmlegoValidator.isValid;
import static ch.sbb.matsim.umlego.metadata.MetadataDayKey.*;

import ch.sbb.matsim.umlego.demand.UnroutableDemandStats;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.metadata.MetadataDayRepository;
import ch.sbb.matsim.umlego.writers.UnroutableDemandStatsWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.time.LocalDate;

public class JdbcUnroutableDemandStatsWriter implements UnroutableDemandStatsWriter {

    private static final Logger LOG = LogManager.getLogger(JdbcUnroutableDemandStatsWriter.class);

    private final Connection connection;
    private final String runId;
    private final LocalDate targetDate;

    public JdbcUnroutableDemandStatsWriter(Connection connection, String runId, LocalDate targetDate) {
        this.connection = connection;
        this.runId = runId;
        this.targetDate = targetDate;
    }

    @Override
    public void write(UnroutableDemandStats stats) throws ZoneNotFoundException {
        LOG.info("Writing unroutable demand metadata to database");
        Double demand = stats.largestUnroutableDemandZone().demand();

        boolean isUmlegoValid = isValid(targetDate, stats.percentUnroutableDemand(), demand);

        MetadataDayRepository metadataDayRepository = new MetadataDayRepository();
        metadataDayRepository.insertMetadata(connection, runId, targetDate, UNROUTABLE_DEMAND, stats.totalUnroutableDemand());
        metadataDayRepository.insertMetadata(connection, runId, targetDate, SHARE_UNROUTABLE_DEMAND, stats.percentUnroutableDemand());
        metadataDayRepository.insertMetadata(connection, runId, targetDate, LARGEST_UNROUTABLE_ZONE, stats.largestUnroutableDemandZone().fromZone());
        metadataDayRepository.insertMetadata(connection, runId, targetDate, DEMAND_LARGEST_UNROUTABLE_ZONE, demand);
        metadataDayRepository.insertMetadata(connection, runId, targetDate, VALID_AUTO, isUmlegoValid);
        metadataDayRepository.insertMetadata(connection, runId, targetDate, VALID_MANUAL, null);
    }
}