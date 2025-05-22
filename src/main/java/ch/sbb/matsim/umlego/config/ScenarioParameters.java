package ch.sbb.matsim.umlego.config;

/**
 * The {@code ScenarioParameters} class represents the configuration parameters for a scenario in the application.
 */
public final class ScenarioParameters {

    /**
     * The name of the scenario.
     */
    private String name;

    /**
     * Path to the MATSim schedule xml file.
     */
    private String scheduleFile;

    /**
     * Path to the MATSim vehicles xml file.
     */
    private String vehiclesFile;

    /**
     * Path to the MATSim network xml file.
     */
    private String networkFile;

    public String getName() {
        return name;
    }

    public ScenarioParameters() {
    }

    public ScenarioParameters(String scheduleFile, String vehiclesFile, String networkFile) {
        this.scheduleFile = scheduleFile;
        this.vehiclesFile = vehiclesFile;
        this.networkFile = networkFile;
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
