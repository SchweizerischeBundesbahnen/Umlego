package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

public class SkimJourneyTime implements SkimCalculator {

    @Override
    public double aggregateRoute(double currentValue, String destZone, FoundRoute route) {
        return currentValue + (route.stop2stopRoute.arrTime - route.stop2stopRoute.depTime);
    }

    @Override
    public SkimColumn getSkimType() {
        return SkimColumn.SUM_JOURNEYTIME;
    }
}
