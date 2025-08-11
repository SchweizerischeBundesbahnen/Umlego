package ch.sbb.matsim.umlego.workflows;

import ch.sbb.matsim.umlego.UmlegoUtils;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.workflows.assignment.Assignment;
import ch.sbb.matsim.umlego.workflows.assignment.AssignmentParameters;
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

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WorkflowRunner.class);
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

        log.

        AssignmentParameters assignmentParameters = config.getConfig(umlegoParameters.workflow().name(), AssignmentParameters.class);

        Assignment assignment = new Assignment(assignmentParameters, umlegoParameters);
        assignment.run();

        return 0;
    }
}
