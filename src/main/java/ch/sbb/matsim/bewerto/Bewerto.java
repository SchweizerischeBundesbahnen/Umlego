package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.bewerto.config.BewertoParameters;
import ch.sbb.matsim.bewerto.config.ElasticitiesParameters;
import ch.sbb.matsim.bewerto.demand.DemandFactorCalculator;
import ch.sbb.matsim.umlego.Umlego;
import ch.sbb.matsim.umlego.UmlegoRunner;
import ch.sbb.matsim.umlego.UmlegoSkimCalculator;
import ch.sbb.matsim.umlego.config.ScenarioParameters;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
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

        for (ScenarioParameters variant : bewertoParameters.getVariants()) {
            processVariant(runner, baseCase, variant);
        }
    }

    private void processVariant(UmlegoRunner runner, Umlego baseCase, ScenarioParameters variant) throws ZoneNotFoundException {

        File outputDir = new File(bewertoParameters.getOutputDir());

        UmlegoRunner variantRunner = new UmlegoRunner(
                new File(outputDir, variant.getName()).getAbsolutePath(),
                bewertoParameters.getZoneConnectionsFile(), variant, runner
        );

        Umlego result = variantRunner.run();

        LOG.info("Variant {} completed. Updating demand...", variant.getName());

        UmlegoSkimCalculator baseSkim = baseCase.getListener(UmlegoSkimCalculator.class);
        UmlegoSkimCalculator variantSkim = result.getListener(UmlegoSkimCalculator.class);

        DemandMatrices updatedDemand = calculateInducedDemand(runner.getDemand(), baseSkim, variantSkim);

        // Assign the demand again
        UmlegoRunner inducedDemandRunner = new UmlegoRunner(
                new File(outputDir, variant.getName() + "-induced").getAbsolutePath(),
                updatedDemand, bewertoParameters.getZoneConnectionsFile(), variantRunner
        );

        inducedDemandRunner.run();
    }

    /**
     * Calculates the induced demand matrices based on the base and variant skims.
     */
    private DemandMatrices calculateInducedDemand(DemandMatrices demand, UmlegoSkimCalculator baseSkim, UmlegoSkimCalculator variantSkim) {

        ElasticitiesParameters elaParameters = bewertoParameters.getElasticities();
        DemandFactorCalculator calculator = new DemandFactorCalculator(elaParameters.getFile(), elaParameters.getSegment(), baseSkim, variantSkim);

        DemandMatrices updatedDemand = new DemandMatrices(demand);

        // Applies the demand factor calculator to the demand matrices
        updatedDemand.multiplyMatrixValues((fromZone, toZone, timeMin) -> calculator.calculateFactor(fromZone, toZone));

        return updatedDemand;
    }

}
