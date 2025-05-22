package ch.sbb.matsim.umlego;

import static ch.sbb.matsim.umlego.ZoneConnections.readZoneConnections;
import static ch.sbb.matsim.umlego.util.PathUtil.ensureDir;

import ch.sbb.matsim.umlego.config.*;
import ch.sbb.matsim.umlego.ZoneConnections.ConnectedStop;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.readers.DemandManager;
import com.google.common.collect.Table;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.builder.GestaltBuilder;
import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.source.ClassPathConfigSourceBuilder;
import org.github.gestalt.config.source.FileConfigSourceBuilder;
import org.github.gestalt.config.source.SystemPropertiesConfigSourceBuilder;
import org.github.gestalt.config.yaml.YamlModuleConfigBuilder;
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

    /**
     * Main method to start Umlego.
     *
     * @param args <br/> [0] baseMatrices path<br/> [1] zonesFilename lookup path<br/> [2] zoneConnectionsFilename matching the given schedule<br/> [3] scheduleFilename<br/> [4]
     *     vehiclesFilename<br/> [5] networkFilename<br/> [6] outputFolder<br/> [7] (vararg) factorFilenames<br/>
     *     <p>
     *     Throws IOException if an I/O error occurs and ZoneNotFoundException if a zone is not found.
     */
    public static void main(String[] args) throws IOException, ZoneNotFoundException, GestaltException {
        if (args.length < 9) {
            throw new IllegalArgumentException("Insufficient arguments provided. Expected at least 8 arguments.");
        }

        String baseMatrices = args[0];
        String zonesFilename = args[1];
        String zoneConnectionsFilename = args[2];
        String scheduleFilename = args[3];
        String vehiclesFilename = args[4];
        String networkFilename = args[5];
        String outputFolder = args[7];

        String[] factorFilenames =
            args.length >= 10 ? Arrays.stream(Arrays.copyOfRange(args, 8, args.length)).map(String::trim)
                .toArray(String[]::new) : new String[0];

        Gestalt config = loadConfig(null);

        UmlegoRunner runner =
            new UmlegoRunner(
                outputFolder,
                zonesFilename,
                zoneConnectionsFilename,
                new ScenarioParameters(scheduleFilename, vehiclesFilename, networkFilename),
                config.getConfig("umlego", UmlegoParameters.class),
                baseMatrices,
                factorFilenames);
        runner.run();
    }

    public UmlegoRunner(
        String outputFolder,
        String zonesFile,
        String zoneConnectionsFile,
        ScenarioParameters scenarioParameters,
        UmlegoParameters umlegoParameters,
        String demandMatricesPath,
        String... factorMatrix) throws IOException, ZoneNotFoundException {
        UmlegoLogger.setOutputFolder(outputFolder);
        ensureDir(outputFolder);
        this.outputFolder = outputFolder;
        this.scenario = loadScenario(scenarioParameters);
        this.stopsPerZone = readConnections(zoneConnectionsFile, scenario.getTransitSchedule());
        this.demand = DemandManager.prepareDemand(zonesFile, demandMatricesPath, factorMatrix);
        this.params = umlegoParameters;
    }

    public UmlegoRunner(
        String outputFolder,
        String zonesFile,
        String zoneConnectionsFile,
        ScenarioParameters scenarioParameters,
        UmlegoParameters umlegoParameters,
        String... factorMatrix) throws IOException, ZoneNotFoundException {
        UmlegoLogger.setOutputFolder(outputFolder);
        ensureDir(outputFolder);
        this.outputFolder = outputFolder;
        this.scenario = loadScenario(scenarioParameters);
        this.stopsPerZone = readConnections(zoneConnectionsFile, scenario.getTransitSchedule());
        this.demand = DemandManager.prepareDemand(zonesFile, factorMatrix);
        this.params = umlegoParameters;
    }

    /**
     * Constructor to re-use UmlegoRunner on a different scenario.
     */
    public UmlegoRunner(String outputFolder, String zoneConnectionsFile, ScenarioParameters scenarioParameters, UmlegoRunner other) {
        UmlegoLogger.setOutputFolder(outputFolder);
        ensureDir(outputFolder);

        this.outputFolder = outputFolder;
        this.scenario = loadScenario(scenarioParameters);
        this.stopsPerZone = readConnections(zoneConnectionsFile, scenario.getTransitSchedule());;
        this.demand = other.demand;
        this.params = other.params;
    }

    public DemandMatrices getDemand() {
        return demand;
    }

    /**
     * Run Umlego and return the created Umlego object.
     */
    public Umlego run() throws ZoneNotFoundException {
        long startTime = System.currentTimeMillis();

        LOG.info("Starting Umlego with the following parameters:");
        LOG.info(params);

        int threads = params.threads() < 0 ? Runtime.getRuntime().availableProcessors() : params.threads();

        // Run Umlego simulation
        Umlego umlego = new Umlego(demand, scenario, stopsPerZone);

        umlego.run(params, threads, outputFolder);

        long endTime = System.currentTimeMillis();
        LOG.info("Total time: {} seconds", (endTime - startTime) / 1000.0);

        return umlego;
    }

    /**
     * Load the parameters from the configuration file.
     * @param configPath optional path to configuration
     */
    public static Gestalt loadConfig(@Nullable Path configPath) throws GestaltException {
        GestaltBuilder builder = new GestaltBuilder()
                .addSource(ClassPathConfigSourceBuilder.builder().setResource("/umlego.yaml").build());

        if (configPath != null) {
          builder.addSource(FileConfigSourceBuilder.builder().setPath(configPath).build());
        }

        Gestalt config = builder.addSource(SystemPropertiesConfigSourceBuilder.builder().build())
                .addModuleConfig(YamlModuleConfigBuilder.builder().build())
                .build();

        config.loadConfigs();

        return config;
    }

    private Scenario loadScenario(ScenarioParameters scenarioParameters) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new TransitScheduleReader(scenario).readFile(scenarioParameters.getScheduleFile());
        new MatsimVehicleReader(scenario.getTransitVehicles()).readFile(scenarioParameters.getVehiclesFile());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(scenarioParameters.getNetworkFile());
        return scenario;
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
