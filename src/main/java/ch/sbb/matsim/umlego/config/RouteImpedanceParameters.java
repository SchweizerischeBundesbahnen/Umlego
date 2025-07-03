package ch.sbb.matsim.umlego.config;

public record RouteImpedanceParameters(
        double betaPerceivedJourneyTime,
        double betaDeltaTEarly,
        double betaDeltaTLate
) {

}
