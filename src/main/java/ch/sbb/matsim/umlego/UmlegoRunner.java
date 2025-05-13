package ch.sbb.matsim.umlego;

import static ch.sbb.matsim.umlego.ZoneConnections.readZoneConnections;
import static ch.sbb.matsim.umlego.util.PathUtil.ensureDir;

import ch.sbb.matsim.umlego.Umlego.PerceivedJourneyTimeParameters;
import ch.sbb.matsim.umlego.Umlego.PreselectionParameters;
import ch.sbb.matsim.umlego.Umlego.RouteImpedanceParameters;
import ch.sbb.matsim.umlego.Umlego.RouteSelectionParameters;
import ch.sbb.matsim.umlego.Umlego.SearchImpedanceParameters;
import ch.sbb.matsim.umlego.Umlego.UmlegoParameters;
import ch.sbb.matsim.umlego.Umlego.WriterParameters;
import ch.sbb.matsim.umlego.ZoneConnections.ConnectedStop;
import ch.sbb.matsim.umlego.config.UmlegoWriterType;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.readers.DemandManager;
import com.google.common.collect.Table;
import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.MatsimVehicleReader;

public class UmlegoRunner {

    private static final Logger LOG = LogManager.getLogger(UmlegoRunner.class);
    private final String outputFolder;
    private final Scenario scenario;
    private final Map<String, List<ConnectedStop>> stopsPerZone;
    private final DemandMatrices demand;
    private final UmlegoParameters params;
    private final int threads;

    /**
     * Main method to start Umlego.
     *
     * @param args <br/> [0] baseMatrices path<br/> [1] zonesFilename lookup path<br/> [2] zoneConnectionsFilename matching the given schedule<br/> [3] scheduleFilename<br/> [4]
     *     vehiclesFilename<br/> [5] networkFilename<br/> [6] threads<br/> [7] outputFolder<br/> [8] writers as comma-separated string the writer names<br/> [9] (vararg) factorFilenames<br/>
     *     <p>
     *     Throws IOException if an I/O error occurs and ZoneNotFoundException if a zone is not found.
     */
    public static void main(String[] args) throws IOException, ZoneNotFoundException {
        if (args.length < 9) {
            throw new IllegalArgumentException("Insufficient arguments provided. Expected at least 8 arguments.");
        }

        String baseMatrices = args[0];
        String zonesFilename = args[1];
        String zoneConnectionsFilename = args[2];
        String scheduleFilename = args[3];
        String vehiclesFilename = args[4];
        String networkFilename = args[5];
        int threads = Integer.parseInt(args[6]);
        String outputFolder = args[7];
        Set<UmlegoWriterType> writersList = Arrays.stream(args[8].split(",")).map(UmlegoWriterType::valueOf)
            .collect(Collectors.toSet());
        String[] factorFilenames =
            args.length >= 10 ? Arrays.stream(Arrays.copyOfRange(args, 9, args.length)).map(String::trim)
                .toArray(String[]::new) : new String[0];

        UmlegoRunner runner =
            new UmlegoRunner(
                outputFolder,
                zonesFilename,
                zoneConnectionsFilename,
                scheduleFilename,
                vehiclesFilename,
                networkFilename,
                writersList,
                threads,
                baseMatrices,
                factorFilenames);
        runner.run();
    }

    public UmlegoRunner(
        String outputFolder,
        String zonesFile,
        String zoneConnectionsFile,
        String scheduleFile,
        String vehiclesFile,
        String networkFile,
        Set<UmlegoWriterType> writers,
        int threads,
        String demandMatricesPath,
        String... factorMatrix) throws IOException, ZoneNotFoundException {
        UmlegoLogger.setOutputFolder(outputFolder);
        ensureDir(outputFolder);
        this.outputFolder = outputFolder;
        this.scenario = loadScenario(scheduleFile, vehiclesFile, networkFile);
        this.stopsPerZone = readConnections(zoneConnectionsFile, scenario.getTransitSchedule());
        this.demand = DemandManager.prepareDemand(zonesFile, demandMatricesPath, factorMatrix);
        this.params = createUmlegoParameters(scenario.getTransitSchedule(), writers);
        this.threads = threads;
    }

    public UmlegoRunner(
        String outputFolder,
        String zonesFile,
        String zoneConnectionsFile,
        String scheduleFile,
        String vehiclesFile,
        String networkFile,
        Set<UmlegoWriterType> writers,
        int threads,
        LocalDate targetDate,
        String... factorMatrix) throws IOException, ZoneNotFoundException {
        UmlegoLogger.setOutputFolder(outputFolder);
        ensureDir(outputFolder);
        this.outputFolder = outputFolder;
        this.scenario = loadScenario(scheduleFile, vehiclesFile, networkFile);
        this.stopsPerZone = readConnections(zoneConnectionsFile, scenario.getTransitSchedule());
        this.demand = DemandManager.prepareDemand(zonesFile, factorMatrix);
        this.params = createUmlegoParameters(scenario.getTransitSchedule(), writers);
        this.threads = threads;
    }

    public void run() throws ZoneNotFoundException {
        long startTime = System.currentTimeMillis();

        // Run Umlego simulation
        new Umlego(demand, scenario, stopsPerZone).run(params, threads, outputFolder);

        long endTime = System.currentTimeMillis();
        LOG.info("Total time: {} seconds", (endTime - startTime) / 1000.0);
    }

    private Scenario loadScenario(String scheduleFile, String vehiclesFile, String networkFile) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new TransitScheduleReader(scenario).readFile(scheduleFile);
        new MatsimVehicleReader(scenario.getTransitVehicles()).readFile(vehiclesFile);
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);
        return scenario;
    }

    private UmlegoParameters createUmlegoParameters(TransitSchedule transitSchedule, Set<UmlegoWriterType> writers) {
        // TODO: store centrally, perhaps using the raptor config
        SearchImpedanceParameters search = new SearchImpedanceParameters(1.0, 1.0, 1.0, 1.0, 1.0, 10.0);
        PreselectionParameters preselection = new PreselectionParameters(2.0, 60.0);
        PerceivedJourneyTimeParameters pjt = new PerceivedJourneyTimeParameters(1.0, 2.94, 2.94, 2.25, 1.13, 17.236, 0.033, 58.0);
        RouteImpedanceParameters impedance = new RouteImpedanceParameters(1.0, 1.85, 1.85);
        RouteSelectionParameters routeSelection = new RouteSelectionParameters(false, 3600.0, 3600.0, RouteUtilityCalculators.boxcox(1.536, 0.5));
        WriterParameters writer = new WriterParameters(1e-5, writers, transitSchedule, List.of());
        return new UmlegoParameters(5, search, preselection, pjt, impedance, routeSelection, writer);
    }

    /**
     * Here, we read the locally generated connection file filledConnections.csv (not the Anbindungs file on Azure).
     *
     * @param zoneConnectionsFilename
     * @param schedule
     * @return Map<String, List < ConnectedStop>>
     */
    private static Map<String, List<ConnectedStop>> readConnections(String zoneConnectionsFilename, TransitSchedule schedule) {
        BufferedReader reader = IOUtils.getBufferedReader(zoneConnectionsFilename);
        Table<String, Id<TransitStopFacility>, ConnectedStop> zoneConnections = readZoneConnections(reader, schedule);
        return zoneConnections.rowMap().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new ArrayList<>(entry.getValue().values())
            ));
    }
}
