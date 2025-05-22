package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.bewerto.config.BewertoParameters;
import ch.sbb.matsim.umlego.Umlego;
import ch.sbb.matsim.umlego.UmlegoRunner;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

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

        LOG.info("Starting Bewerto with parameters: {}", bewertoParameters);

        File outputDir = new File(bewertoParameters.getOutputDir());

        UmlegoRunner runner = new UmlegoRunner(new File(outputDir, "base").getAbsolutePath(),
                bewertoParameters.getZoneNamesFile(),
                bewertoParameters.getZoneConnectionsFile(),
                bewertoParameters.getRef(),
                umlegoParameters,
                bewertoParameters.getDemandFile(), new String[0]
        );

        Umlego baseCase = runner.run();

        LOG.info("Base case completed. Starting variants...");


    }
}
