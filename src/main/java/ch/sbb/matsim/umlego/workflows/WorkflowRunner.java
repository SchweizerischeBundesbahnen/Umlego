package ch.sbb.matsim.umlego.workflows;

import ch.sbb.matsim.umlego.UmlegoUtils;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.workflows.assignment.Assignment;
import ch.sbb.matsim.umlego.workflows.assignment.AssignmentParameters;
import ch.sbb.matsim.umlego.workflows.bewerto.Bewerto;
import ch.sbb.matsim.umlego.workflows.bewerto.config.BewertoParameters;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.github.gestalt.config.Gestalt;
import picocli.CommandLine;

@CommandLine.Command(
    name = "Umlego Workflow",
    description = "Umlego Workflow command line tool",
    mixinStandardHelpOptions = true
)
public final class WorkflowRunner implements Callable<Integer> {

    @CommandLine.Option(
        names = {"-c", "--config"},
        description = "Path to the configuration file",
        required = true
    )
    private Path configPath;

    public static void main(String[] args) {
        new CommandLine(new WorkflowRunner())
            .setCaseInsensitiveEnumValuesAllowed(true)
            .execute(args);
    }

    @Override
    public Integer call() throws Exception {

        Gestalt config = UmlegoUtils.loadConfig(configPath);

        UmlegoParameters umlegoParameters = config.getConfig("umlego", UmlegoParameters.class);

        if (umlegoParameters.workflow().equals(WorkflowEnum.assignment)) {
            AssignmentParameters assignmentParameters = config.getConfig(umlegoParameters.workflow().name(), AssignmentParameters.class);
            Assignment assignment = new Assignment(assignmentParameters, umlegoParameters);
            assignment.run();
        } else if (umlegoParameters.workflow().equals(WorkflowEnum.bewerto)) {
            BewertoParameters bewertoParameters = config.getConfig(umlegoParameters.workflow().name(), BewertoParameters.class);
            Bewerto bewerto = new Bewerto(bewertoParameters, umlegoParameters);
            bewerto.run();
        } else {
            throw new IllegalArgumentException("Unknown workflow: " + umlegoParameters.workflow());
        }

        return 0;
    }
}
