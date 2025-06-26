package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.bewerto.config.BewertoParameters;
import ch.sbb.matsim.umlego.Umlego;
import ch.sbb.matsim.umlego.UmlegoLogger;
import ch.sbb.matsim.umlego.UmlegoRunner;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.readers.DemandManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;

import java.nio.file.Files;
import java.nio.file.Path;

import static ch.sbb.matsim.umlego.util.PathUtil.ensureDir;

/**
 * The {@code Bewerto} class serves the core functionality of the Bewerto application.
 */
public final class Bewerto {

    private static final Logger LOG = LogManager.getLogger(Bewerto.class);

    private final BewertoParameters bewertoParameters;
    private final UmlegoParameters umlegoParameters;

    /**
     * The main constructor for the Bewerto class.
     */
    public Bewerto(BewertoParameters bewertoParameters, UmlegoParameters umlegoParameters) {
        this.bewertoParameters = bewertoParameters;
        this.umlegoParameters = umlegoParameters;
    }

    public void run() throws Exception {

        if (!Files.exists(Path.of(bewertoParameters.getElasticities().getFile())))
            throw new IllegalArgumentException("Elasticities file does not exist: " + bewertoParameters.getElasticities().getFile());

        String outputFolder = bewertoParameters.getOutputDir();
        UmlegoLogger.setOutputFolder(outputFolder);
        ensureDir(outputFolder);

        LOG.info("Starting Bewerto with parameters: {}", bewertoParameters);

        Scenario scenario = UmlegoRunner.loadScenario(bewertoParameters.getRef());

        DemandMatrices demand = DemandManager.prepareDemand(bewertoParameters.getZoneNamesFile(), bewertoParameters.getDemandFile(), new String[0]);

        BewertoWorkflowFactory workflow = new BewertoWorkflowFactory(bewertoParameters, demand, bewertoParameters.getZoneConnectionsFile(), scenario,
                bewertoParameters.getVariants().stream().map(UmlegoRunner::loadScenario).toList());

        Umlego umlego = new Umlego(demand, workflow);

        int threads = umlegoParameters.threads() < 0 ? Runtime.getRuntime().availableProcessors() : umlegoParameters.threads();

        umlego.run(umlegoParameters, threads, outputFolder);
    }

}
