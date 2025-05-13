package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.config.UmlegoConfig;
import ch.sbb.matsim.umlego.config.UmlegoWriterType;
import ch.sbb.matsim.umlego.config.SaisonRunIDNotOnSnowflakeException;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.util.PathUtil;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static ch.sbb.matsim.umlego.config.UmlegoConfig.isRunningLocally;

/**
 * Factory class returing UmlegoRunner depending on configuration.
 */
public class UmlegoRunnerFactory {

    private static final Logger LOG = LogManager.getLogger(UmlegoRunnerFactory.class);

    public UmlegoRunner createRunner(UmlegoConfig config, String scheduleFolder, LocalDate targetDate, int index) throws IOException, ZoneNotFoundException, SaisonRunIDNotOnSnowflakeException {
        Set<UmlegoWriterType> writersList = config.writers();
        String demandMatricesPath = config.getDemandMatricesPath(targetDate);
        String[] factorFilenames = config.getFactorMatricesFileNames(targetDate);

        if (isRunningLocally()) {
            return new UmlegoRunner(
                    PathUtil.getLocalOutputFolder(config, targetDate, index),
                    config.zones(),
                    getConnectionsFile(scheduleFolder),
                    getScheduleFile(scheduleFolder),
                    getVehiclesFile(scheduleFolder),
                    getNetworkFile(scheduleFolder),
                    writersList,
                    config.threads(),
                    demandMatricesPath,
                    factorFilenames
            );
        } else {
            String saison = config.calendar().getCalendarDayRecord(targetDate).baseDemand();
            String saisonRunId = config.simbaRunId() + "_" + saison;
            return new UmlegoRunner(
                    config.runId(),
                    PathUtil.getRemoteOutputFolder(config.runId(), config.year(), targetDate),
                    config.zones(),
                    getConnectionsFile(scheduleFolder),
                    getScheduleFile(scheduleFolder),
                    getVehiclesFile(scheduleFolder),
                    getNetworkFile(scheduleFolder),
                    writersList,
                    config.threads(),
                    saisonRunId,
                    saison,
                    targetDate,
                    factorFilenames
            );
        }
    }

    private String getNetworkFile(String scheduleFolder) {
        return Paths.get(scheduleFolder, "transitNetwork.xml.gz").toString();
    }

    private String getVehiclesFile(String scheduleFolder) {
        return Paths.get(scheduleFolder, "transitVehicles.xml.gz").toString();
    }

    private String getScheduleFile(String scheduleFolder) {
        return Paths.get(scheduleFolder, "transitSchedule.xml.gz").toString();
    }

    private String getConnectionsFile(String scheduleFolder) {
        return Paths.get(scheduleFolder, "filledConnections.csv").toString();
    }
}
