package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.FoundRoute;

public class SkimWeightedJourneyTime implements SkimCalculator {

    @Override
    public double aggregateRoute(double currentValue, String destZone, FoundRoute route) {
        var demand = route.demand;
        return currentValue + demand * (route.stop2stopRoute.arrTime - route.stop2stopRoute.depTime);
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
