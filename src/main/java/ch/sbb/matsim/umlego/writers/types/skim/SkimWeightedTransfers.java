package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

public class SkimWeightedTransfers implements SkimCalculator {

    @Override
    public double aggregateRoute(double currentValue, String destZone, FoundRoute route) {
        var demand = route.demand;
        return currentValue + demand * route.stop2stopRoute.transfers;
    }

    @Override
    public SkimColumn getSkimType() {
        return SkimColumn.SUM_WEIGHTED_TRANSFERS;
    }
}
