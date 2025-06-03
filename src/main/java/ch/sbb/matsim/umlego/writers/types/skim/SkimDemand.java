package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

public class SkimDemand implements SkimCalculator {

    @Override
    public double aggregateRoute(double currentValue, String destZone, FoundRoute route) {
        return currentValue + route.demand;
    }

    @Override
    public SkimColumn getSkimType() {
        return SkimColumn.DEMAND;
    }
}
