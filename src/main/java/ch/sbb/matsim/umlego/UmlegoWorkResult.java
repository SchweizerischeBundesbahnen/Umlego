package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.demand.UnroutableDemand;

import java.util.List;
import java.util.Map;

/**
 * WorkResult represents the result of processing a {@link WorkItem}.
 */
public record UmlegoWorkResult(
        String originZone,
        Map<String, List<FoundRoute>> routesPerDestinationZone,
        UnroutableDemand unroutableDemand
) implements WorkResult {
}
