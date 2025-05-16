package ch.sbb.matsim.bewerto.config;

/**
 * The {@code ScenarioParameters} class represents the configuration parameters for a scenario in the Bewerto application.
 */
public final class ScenarioParameters {

    /**
     * The name of the scenario.
     */
    private String name;

    /**
     * Path to the MATSIm schedule xml file.
     */
    private String scheduleFile;

    /**
     * Path to the MATSIm vehicles xml file.
     */
    private String vehiclesFile;

    /**
     * Path to the MATSIm network xml file.
     */
    private String networkFile;

    public String getName() {
        return name;
    }

    public ScenarioParameters setName(String name) {
        this.name = name;
        return this;
    }

    public String getScheduleFile() {
        return scheduleFile;
    }

    public ScenarioParameters setScheduleFile(String scheduleFile) {
        this.scheduleFile = scheduleFile;
        return this;
    }

    public String getVehiclesFile() {
        return vehiclesFile;
    }

    public ScenarioParameters setVehiclesFile(String vehiclesFile) {
        this.vehiclesFile = vehiclesFile;
        return this;
    }

    public String getNetworkFile() {
        return networkFile;
    }

    public ScenarioParameters setNetworkFile(String networkFile) {
        this.networkFile = networkFile;
        return this;
    }

    @Override
    public String toString() {
        return "ScenarioParameters{" +
                "name='" + name + '\'' +
                ", scheduleFile='" + scheduleFile + '\'' +
                ", vehiclesFile='" + vehiclesFile + '\'' +
                ", networkFile='" + networkFile + '\'' +
                '}';
    }
}
