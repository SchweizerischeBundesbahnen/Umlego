package ch.sbb.matsim.umlego.workflows.assignment;

import ch.sbb.matsim.umlego.config.ScenarioParameters;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class AssignmentParameters {

    private String outputDir;
    private ScenarioParameters scenario;

    @Override
    public String toString() {
        return "BewertoParameters{" +
            ", outputDir='" + outputDir + '\'' +
            ", scenario=" + scenario +
            '}';
    }
}
