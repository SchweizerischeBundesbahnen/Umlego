package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

public class SkimWeightedJourneyTime implements SkimCalculator {

    @Override
    public double aggregateRoute(double currentValue, String destZone, FoundRoute route) {
        var demand = route.demand.getDouble(destZone);
        return currentValue + demand * (route.arrTime - route.depTime);
    }

    @Override
    public SkimType getSkimType() {
        return SkimType.SUM_WEIGHTED_JOURNEYTIME;
    }
}
