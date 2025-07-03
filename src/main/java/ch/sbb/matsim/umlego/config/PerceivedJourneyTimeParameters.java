package ch.sbb.matsim.umlego.config;

public record PerceivedJourneyTimeParameters(
        double betaInVehicleTime,
        double betaAccessTime,
        double betaEgressTime,
        double betaWalkTime,
        double betaTransferWaitTime,
        double transferFix,
        double transferTraveltimeFactor,
        double secondsPerAdditionalStop
) {

}
