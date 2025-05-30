package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

public class SkimWeightedAdaptationTime implements SkimCalculator {

    @Override
    public double aggregateRoute(double currentValue, String destZone, FoundRoute route) {
        // The adaptation time is already weighted during calculation of the routes
        return currentValue + route.adaptationTime.getDouble(destZone);
    }

    @Override
    public SkimColumn getSkimType() {
        return SkimColumn.SUM_WEIGHTED_ADAPTATION_TIME;
    }
}
