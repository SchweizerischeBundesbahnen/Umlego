package ch.sbb.matsim.bewerto.demand;

import ch.sbb.matsim.umlego.UmlegoSkimCalculator;
import ch.sbb.matsim.umlego.writers.types.skim.ODPair;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

/**
 * The {@code DemandFactorCalculator} class is responsible for calculating demand factors.
 *
 * The calculator uses two {@link UmlegoSkimCalculator} instances to compute the demand factors based on elasticities.
 */
public final class DemandFactorCalculator {

    /**
     * Empty array to represent no data.
     */
    private final static double[] EMPTY = new double[0];

    private final UmlegoSkimCalculator baseSkim;
    private final UmlegoSkimCalculator variantSkim;

    /**
     * Cache for calculated factors.
     */
    private final Object2DoubleMap<ODPair> factors = new Object2DoubleOpenHashMap<>();

    /**
     * Constructs a {@code DemandFactorCalculator} used to calculate demand factors
     * by comparing base and variant skim matrices for given origin-destination zones.
     *
     * @param file the file path containing demand elasticity parameters
     * @param segment the specific demand segment to be applied
     * @param baseSkim the base skim calculator containing metrics for the base scenario
     * @param variantSkim the variant skim calculator containing metrics for the variant scenario
     */
    public DemandFactorCalculator(String file, String segment, UmlegoSkimCalculator baseSkim, UmlegoSkimCalculator variantSkim) {
        this.baseSkim = baseSkim;
        this.variantSkim = variantSkim;
    }

    /**
     * Retrieves the demand factor for a given origin-destination pair.
     */
    public double calculateFactor(String origZone, String destZone) {
        return factors.computeIfAbsent(new ODPair(origZone, destZone), this::computeFactor);
    }

    /**
     * Computation of the demand factor.
     */
    private double computeFactor(ODPair od) {

        double[] variantValues = variantSkim.getSkims().getOrDefault(od, EMPTY);
        if (variantValues == EMPTY) {
            return 1;
        }

        double[] baseValues = baseSkim.getSkims().getOrDefault(od, EMPTY);
        if (baseValues == EMPTY) {
            return 1;
        }

        // TODO: Calculate all the elasticities here
        return 0.5;
    }

}
