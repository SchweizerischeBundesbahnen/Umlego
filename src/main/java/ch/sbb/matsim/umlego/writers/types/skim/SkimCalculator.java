package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

/**
 * The SkimCalculator interface defines the contract for calculating skims based on routes and destination zones.
 * Implementations of this interface will provide specific aggregation logic for different skim types.
 */
public interface SkimCalculator {

    double aggregateRoute(double currentValue, String destZone, FoundRoute route);

    SkimType getSkimType();

    /**
     * Indicates whether the skim calculator is weighted by demand, If true, it will be normalized by the total demand for the origin zone.
     */
    default boolean isNormalizedByDemand() {
        return false;
    }

}
