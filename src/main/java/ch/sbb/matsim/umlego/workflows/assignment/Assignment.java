package ch.sbb.matsim.umlego.workflows.assignment;

import static ch.sbb.matsim.umlego.util.PathUtil.ensureDir;

import ch.sbb.matsim.umlego.Umlego;
import ch.sbb.matsim.umlego.UmlegoLogger;
import ch.sbb.matsim.umlego.UmlegoUtils;
import ch.sbb.matsim.umlego.config.MatricesParameters;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.readers.DemandManager;
import ch.sbb.matsim.umlego.readers.MatrixFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;

/**
 * The {@code Bewerto} class serves the core functionality of the Bewerto application.
 */
public final class Assignment {

    private static final Logger LOG = LogManager.getLogger(Assignment.class);

    private final AssignmentParameters assignmentParameters;
    private final UmlegoParameters umlegoParameters;
    private final MatricesParameters matricesParameters;

    /**
     * The main constructor for the Bewerto class.
     */
    public Assignment(AssignmentParameters assignmentParameters, UmlegoParameters umlegoParameters, MatricesParameters matricesParameters) {
        this.assignmentParameters = assignmentParameters;
        this.umlegoParameters = umlegoParameters;
        this.matricesParameters = matricesParameters;
    }

    public void run() throws Exception {

        long startTime = System.currentTimeMillis();

        String outputFolder = assignmentParameters.getOutputDir();
        UmlegoLogger.setOutputFolder(outputFolder);
        ensureDir(outputFolder);

        LOG.info("Starting Assignment with parameters: {}", assignmentParameters);

        Scenario scenario = UmlegoUtils.loadScenario(assignmentParameters.getScenario());

        MatrixFactory matrixFactory = new MatrixFactory(this.matricesParameters);
        Matrices demand = DemandManager.prepareDemand(matricesParameters.zoneNamesFile(), matricesParameters.matrixFile(), matrixFactory, new String[0]);

        AssignmentWorkflowFactory workflow = new AssignmentWorkflowFactory(demand, matricesParameters.zoneConnectionsFile(), scenario);

        Umlego umlego = new Umlego(demand, workflow);

        int threads = umlegoParameters.threads() < 0 ? Runtime.getRuntime().availableProcessors() : umlegoParameters.threads();

        umlego.run(umlegoParameters, threads, outputFolder);

        long endTime = System.currentTimeMillis();
        LOG.info("Total time: {} seconds", (endTime - startTime) / 1000.0);

    }

}
