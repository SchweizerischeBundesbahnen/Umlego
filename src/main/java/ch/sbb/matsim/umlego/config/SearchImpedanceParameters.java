package ch.sbb.matsim.umlego.config;

public record SearchImpedanceParameters(
        double betaInVehicleTime,
        double betaAccessTime,
        double betaEgressTime,
        double betaWalkTime,
        double betaTransferWaitTime,
        double betaTransferCount
) {

}
