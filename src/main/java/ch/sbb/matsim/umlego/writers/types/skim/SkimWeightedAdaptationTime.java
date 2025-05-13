package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

public class SkimWeightedAdaptationTime implements SkimCalculator {

    @Override
    public double aggregateRoute(double currentValue, String destZone, FoundRoute route) {
        var demand = route.demand.getDouble(destZone);
        return currentValue + demand * route.adaptationTime.getDouble(destZone);
    }

    @Override
    public SkimType getSkimType() {
        return SkimType.SUM_WEIGHTED_ADAPTATION_TIME;
    }
}
