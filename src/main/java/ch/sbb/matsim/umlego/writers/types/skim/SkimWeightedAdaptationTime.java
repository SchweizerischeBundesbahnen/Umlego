package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

/**
 * Calculate weighted adaptation time for a skim (in minutes).
 */
public class SkimWeightedAdaptationTime implements SkimCalculator {

    @Override
    public double aggregateRoute(double currentValue, String destZone, FoundRoute route) {
        // The adaptation time is already weighted during calculation of the routes
        return currentValue + route.adaptationTime / 60; // Convert seconds to minutes
    }

    @Override
    public SkimColumn getSkimType() {
        return SkimColumn.SUM_WEIGHTED_ADAPTATION_TIME;
    }

    @Override
    public boolean isNormalizedByDemand() {
        return true;
    }
}
