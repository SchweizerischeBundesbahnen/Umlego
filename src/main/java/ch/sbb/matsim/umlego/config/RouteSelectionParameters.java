package ch.sbb.matsim.umlego.config;

import ch.sbb.matsim.umlego.RouteUtilityCalculator;

public record RouteSelectionParameters(
        boolean limitSelectionToTimewindow,
        double beforeTimewindow,
        double afterTimewindow,
        UtilityFunctionParams utilityCalculator
) {

}
