package ch.sbb.matsim.umlego.demand;

public record UnroutableDemandPart(
        String fromZone,
        String toZone,
        double demand) {
}