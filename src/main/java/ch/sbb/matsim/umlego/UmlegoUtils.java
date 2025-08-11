package ch.sbb.matsim.umlego;

import static ch.sbb.matsim.umlego.Connectors.readZoneConnectors;

import ch.sbb.matsim.umlego.Connectors.ConnectedStop;
import ch.sbb.matsim.umlego.config.ScenarioParameters;
import com.google.common.collect.Table;
import jakarta.annotation.Nullable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.MatsimVehicleReader;

public class UmlegoUtils {

    /**
     * Load the parameters from the configuration file.
     *
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

    /**
     * Load the scenario based on the provided parameters.
     */
    public static Scenario loadScenario(ScenarioParameters scenarioParameters) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new TransitScheduleReader(scenario).readFile(scenarioParameters.getScheduleFile());
        new MatsimVehicleReader(scenario.getTransitVehicles()).readFile(scenarioParameters.getVehiclesFile());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(scenarioParameters.getNetworkFile());
        scenario.getConfig().controller().setRunId(scenarioParameters.getName());
        return scenario;
    }

    /**
     * Here, we read the locally generated connection file filledConnections.csv (not the Anbindungs file on Azure).
     *
     * @return Map<String, List < ConnectedStop>>
     */
    public static Map<String, List<ConnectedStop>> readConnectors(String zoneConnectionsFilename, TransitSchedule schedule) throws IOException {
        Table<String, Id<TransitStopFacility>, ConnectedStop> zoneConnections = readZoneConnectors(zoneConnectionsFilename, schedule);
        return zoneConnections.rowMap().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> new ArrayList<>(entry.getValue().values())
            ));
    }
}
