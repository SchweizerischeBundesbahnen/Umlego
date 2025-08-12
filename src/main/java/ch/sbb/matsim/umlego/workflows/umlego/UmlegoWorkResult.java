package ch.sbb.matsim.umlego.workflows.umlego;

import ch.sbb.matsim.umlego.FoundRoute;
import ch.sbb.matsim.umlego.demand.UnroutableDemand;

import ch.sbb.matsim.umlego.workflows.interfaces.WorkItem;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkResult;
import java.util.List;
import java.util.Map;

/**
 * WorkResult represents the result of processing a {@link WorkItem}.
 *
 * @param originZone               the zone from which the work was initiated
 * @param routesPerDestinationZone a map of destination zones to lists of found routes
 * @param skims                    a map of destination zones to arrays of skims (travel times, distances, etc.)
 * @param unroutableDemand         the demand that could not be routed
 */
public record UmlegoWorkResult(
        String originZone,
        Map<String, List<FoundRoute>> routesPerDestinationZone,
        Map<String, double[]> skims,
        UnroutableDemand unroutableDemand
) implements WorkResult {
}
