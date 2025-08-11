package ch.sbb.matsim.umlego.workflows.assignment;

import ch.sbb.matsim.umlego.config.ScenarioParameters;

/**
 * The {@code BewertoParameters} class represents the configuration parameters for the Bewerto application.
 */
public final class AssignmentParameters {

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
     * Reference scenario parameters.
     */
    private ScenarioParameters scenario;

    public String getOutputDir() {
        return outputDir;
    }

    public AssignmentParameters setOutputDir(String outputDir) {
        this.outputDir = outputDir;
        return this;
    }

    public String getDemandFile() {
        return demandFile;
    }

    public AssignmentParameters setDemandFile(String demandFile) {
        this.demandFile = demandFile;
        return this;
    }

    public String getZoneNamesFile() {
        return zoneNamesFile;
    }

    public AssignmentParameters setZoneNamesFile(String zoneNamesFile) {
        this.zoneNamesFile = zoneNamesFile;
        return this;
    }

    public String getZoneConnectionsFile() {
        return zoneConnectionsFile;
    }

    public AssignmentParameters setZoneConnectionsFile(String zoneConnectionsFile) {
        this.zoneConnectionsFile = zoneConnectionsFile;
        return this;
    }

    public ScenarioParameters getScenario() {
        return scenario;
    }

    public AssignmentParameters setScenario(ScenarioParameters ref) {
        this.scenario = ref;
        return this;
    }

    @Override
    public String toString() {
        return "BewertoParameters{" +
            "demandFile='" + demandFile + '\'' +
            ", zoneNamesFile='" + zoneNamesFile + '\'' +
            ", zoneConnectionsFile='" + zoneConnectionsFile + '\'' +
            ", outputDir='" + outputDir + '\'' +
            ", scenario=" + scenario +
            '}';
    }
}
