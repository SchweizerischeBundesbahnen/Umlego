package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

public class SkimNumberOfRoutes implements SkimCalculator {

    @Override
    public double aggregateRoute(double currentValue, String destZone, FoundRoute route) {
        return currentValue + 1;
    }

    @Override
    public SkimType getSkimType() {
        return SkimType.NUMBEROFROUTES;
    }
}
