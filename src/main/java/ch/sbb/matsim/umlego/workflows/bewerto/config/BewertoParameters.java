package ch.sbb.matsim.umlego.workflows.bewerto.config;

import ch.sbb.matsim.umlego.config.ScenarioParameters;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * The {@code BewertoParameters} class represents the configuration parameters for the Bewerto application.
 */
@Getter
@Setter
public final class BewertoParameters {

    /**
     * Path to demand matrices.
     */
    private String demandFile;

    /**
     * Path to zone names csv.
     */
    private String zoneNamesFile;

    /**
     * Path to zone connections csv.
     */
    private String zoneConnectionsFile;
    /**
     * The directory path for the output files.
     */
    private String outputDir;
    /**
     * Elasticities parameters for demand modeling.
     */
    private ElasticitiesParameters elasticities;
    /**
     * Reference scenario parameters.
     */
    private ScenarioParameters ref;
    /**
     * List of variant scenario parameters.
     */
    private List<ScenarioParameters> variants;

    @Override
    public String toString() {
        return "BewertoParameters{" +
            "demandFile='" + demandFile + '\'' +
            ", zoneNamesFile='" + zoneNamesFile + '\'' +
            ", zoneConnectionsFile='" + zoneConnectionsFile + '\'' +
            ", outputDir='" + outputDir + '\'' +
            ", elasticities=" + elasticities +
            ", ref=" + ref +
            ", variants=" + variants +
            '}';
    }
}
