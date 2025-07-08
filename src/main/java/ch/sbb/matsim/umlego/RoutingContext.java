package ch.sbb.matsim.umlego;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;
import java.util.Map;

/**
 * Context for routing operations, encapsulating the necessary parameters and data structures.
 */
public record RoutingContext(
        SwissRailRaptor raptor,
        RaptorParameters raptorParams,
        Map<String, List<Connectors.ConnectedStop>> stopsPerZone,
        Map<String, Map<TransitStopFacility, Connectors.ConnectedStop>> stopLookupPerDestination
) {
}
