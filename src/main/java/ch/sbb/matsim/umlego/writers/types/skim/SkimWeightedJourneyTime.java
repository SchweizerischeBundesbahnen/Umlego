package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

/**
 * Calculate weighted journey time for a skim (in minutes).
 */
public class SkimWeightedJourneyTime implements SkimCalculator {

    @Override
    public double aggregateRoute(double currentValue, String destZone, FoundRoute route) {
        var demand = route.demand;
        return currentValue + demand * (route.travelTimeWithAccess / 60);
    }

    @Override
    public SkimColumn getSkimType() {
        return SkimColumn.SUM_WEIGHTED_JOURNEYTIME;
    }

    @Override
    public boolean isNormalizedByDemand() {
        return true;
    }
}
