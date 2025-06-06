package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.demand.UnroutableDemand;

import java.util.List;
import java.util.Map;

public record WorkResult(
        String originZone,
        Map<String, List<FoundRoute>> routesPerDestinationZone,
        UnroutableDemand unroutableDemand
) {
}
