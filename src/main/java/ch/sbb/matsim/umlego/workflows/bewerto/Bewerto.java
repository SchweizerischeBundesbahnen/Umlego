package ch.sbb.matsim.umlego.workflows.bewerto;

import static ch.sbb.matsim.umlego.util.PathUtil.ensureDir;

import ch.sbb.matsim.umlego.Umlego;
import ch.sbb.matsim.umlego.UmlegoLogger;
import ch.sbb.matsim.umlego.UmlegoUtils;
import ch.sbb.matsim.umlego.config.MatricesParameters;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.readers.DemandManager;
import ch.sbb.matsim.umlego.readers.MatrixFactory;
import ch.sbb.matsim.umlego.workflows.bewerto.config.BewertoParameters;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;

/**
 * The {@code Bewerto} class serves the core functionality of the Bewerto application.
 */
public final class Bewerto {

    private static final Logger LOG = LogManager.getLogger(Bewerto.class);

    private final BewertoParameters bewertoParameters;
    private final UmlegoParameters umlegoParameters;
    private final MatricesParameters matricesParameters;

    /**
     * The main constructor for the Bewerto class.
     */
    public Bewerto(BewertoParameters bewertoParameters, UmlegoParameters umlegoParameters, MatricesParameters matricesParameters) {
        this.bewertoParameters = bewertoParameters;
        this.umlegoParameters = umlegoParameters;
        this.matricesParameters = matricesParameters;
    }

    public void run() throws Exception {

        if (!Files.exists(Path.of(bewertoParameters.getElasticities().getFile()))) {
            throw new IllegalArgumentException("Elasticities file does not exist: " + bewertoParameters.getElasticities().getFile());
        }

        long startTime = System.currentTimeMillis();

        String outputFolder = bewertoParameters.getOutputDir();
        UmlegoLogger.setOutputFolder(outputFolder);
        ensureDir(outputFolder);

        LOG.info("Starting Bewerto with parameters: {}", bewertoParameters);

        Scenario scenario = UmlegoUtils.loadScenario(bewertoParameters.getRef());

        MatrixFactory matrixFactory = new MatrixFactory(matricesParameters);
        Matrices demand = DemandManager.prepareDemand(matricesParameters.zoneNamesFile(), matricesParameters.matrixFile(), matrixFactory, new String[0]);

        BewertoWorkflowFactory workflow = new BewertoWorkflowFactory(bewertoParameters, demand, matricesParameters.zoneConnectionsFile(), scenario,
            bewertoParameters.getVariants().stream().map(UmlegoUtils::loadScenario).toList());

        Umlego umlego = new Umlego(demand, workflow);

        int threads = umlegoParameters.threads() < 0 ? Runtime.getRuntime().availableProcessors() : umlegoParameters.threads();

        umlego.run(umlegoParameters, threads, outputFolder);

        long endTime = System.currentTimeMillis();
        LOG.info("Total time: {} seconds", (endTime - startTime) / 1000.0);

    }

}
