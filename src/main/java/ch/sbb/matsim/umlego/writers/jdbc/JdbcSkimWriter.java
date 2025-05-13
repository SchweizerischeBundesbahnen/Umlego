package ch.sbb.matsim.umlego.writers.jdbc;

import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.closeConnection;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;
import ch.sbb.matsim.umlego.writers.UmlegoWriterInterface;
import ch.sbb.matsim.umlego.writers.jdbc.SkimRepository.SkimEntry;
import ch.sbb.matsim.umlego.writers.types.skim.ODPair;
import ch.sbb.matsim.umlego.writers.types.skim.Skim;
import ch.sbb.matsim.umlego.writers.types.skim.SkimCalculator;
import ch.sbb.matsim.umlego.writers.types.skim.SkimDemand;
import ch.sbb.matsim.umlego.writers.types.skim.SkimJourneyTime;
import ch.sbb.matsim.umlego.writers.types.skim.SkimNumberOfRoutes;
import ch.sbb.matsim.umlego.writers.types.skim.SkimType;
import ch.sbb.matsim.umlego.writers.types.skim.SkimWeightedAdaptationTime;
import ch.sbb.matsim.umlego.writers.types.skim.SkimWeightedJourneyTime;
import ch.sbb.matsim.umlego.writers.types.skim.SkimWeightedTransfers;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JdbcSkimWriter implements UmlegoWriterInterface {

    private static final Logger LOG = LogManager.getLogger(JdbcSkimWriter.class);

    private final Set<SkimCalculator> skimCalculators;
    private final Skim skims;
    private final Connection connection;
    private final String runId;
    private final LocalDate targetDate;
    private final SkimRepository repository = new SkimRepository();
    private final List<SkimEntry> entries = new ArrayList<>();

    public JdbcSkimWriter(Connection connection, String runId, LocalDate targetDate) {
        this.connection = connection;
        this.runId = runId;
        this.targetDate = targetDate;
        this.skims = new Skim();
        this.skimCalculators = new HashSet<>();

        // Initialize SkimCalculators
        this.skimCalculators.add(new SkimDemand());
        this.skimCalculators.add(new SkimJourneyTime());
        this.skimCalculators.add(new SkimNumberOfRoutes());
        this.skimCalculators.add(new SkimWeightedJourneyTime());
        this.skimCalculators.add(new SkimWeightedTransfers());
        this.skimCalculators.add(new SkimWeightedAdaptationTime());
    }

    @Override
    public void writeRoute(String origZone, String destZone, FoundRoute route) {
        var key = new ODPair(origZone, destZone);

        var matrices = this.skims.getOrDefault(key, new HashMap<>());
        for (var calculator : this.skimCalculators) {
            var value = matrices.getOrDefault(calculator.getSkimType(), 0.0);
            value = calculator.aggregateRoute(value, destZone, route);
            matrices.put(calculator.getSkimType(), value);
        }
        this.skims.put(key, matrices);
    }

    public Skim getSkims() {
        return this.skims;
    }

    @Override
    public void close() throws Exception {
        LOG.info("Writing Skim entries to database...");

        for (ODPair odPair : skims.keySet()) {
            var matrices = this.skims.get(odPair);

            SkimEntry entry = new SkimEntry(
                runId,
                targetDate,
                Integer.parseInt(odPair.fromZone()),
                Integer.parseInt(odPair.toZone()),
                matrices.get(SkimType.DEMAND),
                matrices.get(SkimType.NUMBEROFROUTES).intValue(),
                matrices.get(SkimType.SUM_JOURNEYTIME).intValue(),
                matrices.get(SkimType.SUM_WEIGHTED_JOURNEYTIME),
                matrices.get(SkimType.SUM_WEIGHTED_TRANSFERS),
                matrices.get(SkimType.SUM_WEIGHTED_ADAPTATION_TIME)
            );

            entries.add(entry);
        }

        repository.insertEntries(connection, entries);
        closeConnection(connection);
        LOG.info("Done writing Skim entries.");
    }
}
