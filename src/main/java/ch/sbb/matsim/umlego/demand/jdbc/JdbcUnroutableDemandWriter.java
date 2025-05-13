package ch.sbb.matsim.umlego.demand.jdbc;

import ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig;
import ch.sbb.matsim.umlego.demand.UnroutableDemand;
import ch.sbb.matsim.umlego.demand.UnroutableDemandWriter;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public class JdbcUnroutableDemandWriter implements UnroutableDemandWriter {

    private final UnroutableDemandRepository repository;
    private final String runId;
    private final LocalDate targetDate;

    public JdbcUnroutableDemandWriter(String runId, LocalDate targetDate) {
        this.runId = runId;
        this.repository = new UnroutableDemandRepository();
        this.targetDate = targetDate;
    }

    @Override
    public void write(UnroutableDemand unroutableDemand) {
        try (Connection connection = SnowflakeConfig.connect()) {
            repository.save(connection, runId, targetDate, unroutableDemand);
        } catch (SQLException e) {
            throw new RuntimeException("Error while connecting to database", e);
        }
    }
}
