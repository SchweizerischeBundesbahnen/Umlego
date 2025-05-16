package ch.sbb.matsim.bewerto.config;

import java.util.List;

/**
 * The {@code BewertoParameters} class represents the configuration parameters for the Bewerto application.
 */
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
     * Number of threads to use for processing.
     */
    private int threads = -1;

    /**
     * Reference scenario parameters.
     */
    private ScenarioParameters ref;

    /**
     * List of variant scenario parameters.
     */
    private List<ScenarioParameters> variants;

    public String getDemandFile() {
        return demandFile;
    }

    public BewertoParameters setDemandFile(String demandFile) {
        this.demandFile = demandFile;
        return this;
    }

    public String getZoneNamesFile() {
        return zoneNamesFile;
    }

    public BewertoParameters setZoneNamesFile(String zoneNamesFile) {
        this.zoneNamesFile = zoneNamesFile;
        return this;
    }

    public String getZoneConnectionsFile() {
        return zoneConnectionsFile;
    }

    public BewertoParameters setZoneConnectionsFile(String zoneConnectionsFile) {
        this.zoneConnectionsFile = zoneConnectionsFile;
        return this;
    }

    public int getThreads() {
        return threads;
    }

    public BewertoParameters setThreads(int threads) {
        this.threads = threads;
        return this;
    }

    public ScenarioParameters getRef() {
        return ref;
    }

    public BewertoParameters setRef(ScenarioParameters ref) {
        this.ref = ref;
        return this;
    }

    public List<ScenarioParameters> getVariants() {
        return variants;
    }

    public BewertoParameters setVariants(List<ScenarioParameters> variants) {
        this.variants = variants;
        return this;
    }

    @Override
    public String toString() {
        return "BewertoParameters{" +
                "demandFile='" + demandFile + '\'' +
                ", zoneNamesFile='" + zoneNamesFile + '\'' +
                ", zoneConnectionsFile='" + zoneConnectionsFile + '\'' +
                ", threads=" + threads +
                ", ref=" + ref +
                ", variants=" + variants +
                '}';
    }
}
