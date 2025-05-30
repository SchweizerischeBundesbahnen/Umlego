package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

public class SkimWeightedTransfers implements SkimCalculator {

    @Override
    public double aggregateRoute(double currentValue, String destZone, FoundRoute route) {
        var demand = route.demand.getDouble(destZone);
        return currentValue + demand * route.transfers;
    }

    @Override
    public SkimColumn getSkimType() {
        return SkimColumn.SUM_WEIGHTED_TRANSFERS;
    }
}
