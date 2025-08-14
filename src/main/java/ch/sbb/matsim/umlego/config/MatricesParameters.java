package ch.sbb.matsim.umlego.config;

import java.util.List;

public record MatricesParameters(

    String matrixFile,
    String zoneNamesFile,
    String zoneConnectionsFile,
    List<DemandMatrixParameter> demandMatrices,
    List<ShareMatrixParameter> shareMatrices
) {

}
