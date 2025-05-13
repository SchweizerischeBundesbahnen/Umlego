package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

public class SkimJourneyTime implements SkimCalculator {

    @Override
    public double aggregateRoute(double currentValue, String destZone, FoundRoute route) {
        return currentValue + (route.arrTime - route.depTime);
    }

    @Override
    public SkimType getSkimType() {
        return SkimType.SUM_JOURNEYTIME;
    }
}
