package ch.sbb.matsim.umlego.demand.jdbc;

import ch.sbb.matsim.umlego.demand.UnroutableDemand;
import ch.sbb.matsim.umlego.demand.UnroutableDemandPart;
import java.sql.Date;
import java.time.LocalDate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UnroutableDemandRepository {

    private static final Logger LOG = LogManager.getLogger(UnroutableDemandRepository.class);

    public void save(Connection connection, String runId, LocalDate targetDate, UnroutableDemand unroutableDemand) {
        String sql = "INSERT INTO unroutable_demand (run_id, target_date, origin, destination, demand) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            LOG.info("Preparing batch write query: {}", sql);
            for (UnroutableDemandPart part : unroutableDemand.getParts()) {
                fillStatement(runId, targetDate, part, ps);
                ps.addBatch();
            }

            int[] batchResults = ps.executeBatch();
            LOG.info("Batch executed successfully, {} rows affected.", batchResults.length);
        } catch (SQLException e) {
            throw new RuntimeException("Error while inserting entries into unroutable_demand", e);
        }
    }

    private void fillStatement(String runId, LocalDate targetDate, UnroutableDemandPart part, PreparedStatement ps) throws SQLException {
        ps.setString(1, runId);
        ps.setDate(2, Date.valueOf(targetDate));
        ps.setString(3, part.fromZone());
        ps.setString(4, part.toZone());
        ps.setDouble(5, part.demand());
    }
}
