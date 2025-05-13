package ch.sbb.matsim.umlego.writers.types.skim;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

public interface SkimCalculator {

    double aggregateRoute(double currentValue, String destZone, FoundRoute route);

    SkimType getSkimType();
}
