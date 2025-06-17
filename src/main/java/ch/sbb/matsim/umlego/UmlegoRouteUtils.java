package ch.sbb.matsim.umlego;

import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for handling routes in the Umlego context.
 */
public class UmlegoRouteUtils {

    private UmlegoRouteUtils() {
        // Utility class, no instantiation
    }

    /**
     * Checks if a route starts with a transfer within the same zone.
     */
    public static boolean routeStartsWithTransferWithinSameZone(Stop2StopRoute route, Set<TransitStopFacility> zoneStops) {
        var firstRoutePart = route.routeParts.getFirst();
        boolean isTransfer = route.originStop != firstRoutePart.fromStop;
        if (isTransfer) {
            // test if both the from and to stop are in the same zone
            return zoneStops.contains(route.originStop) && zoneStops.contains(firstRoutePart.fromStop);
        }
        return false;
    }

    /**
     * Checks if a route ends with a transfer within the same zone.
     */
    public static boolean routeEndsWithTransferWithinSameZone(Stop2StopRoute route, Set<TransitStopFacility> zoneStops) {
        var lastRoutePart = route.routeParts.getLast();
        while (lastRoutePart.chainedPart != null) {
            lastRoutePart = lastRoutePart.chainedPart;
        }

        boolean isTransfer = route.destinationStop != lastRoutePart.toStop;
        if (isTransfer) {
            // test if both the from and to stop are in the same zone
            return zoneStops.contains(route.destinationStop) && zoneStops.contains(lastRoutePart.toStop);
        }
        return false;
    }

    /**
     * Removes dominated routes from the list of routes.
     * A route is dominated if there is another route that has a better or equal departure time,
     * an earlier arrival time, fewer transfers, and a better search impedance.
     */
    public static void removeDominatedRoutes(List<FoundRoute> routes) {
        // sort ascending by departure time, then descending by arrival time
        // if a later route is fully contained in an earlier route, remove the earlier route except it is a direct route (no transfers)
        routes.sort((o1, o2) -> {
            if (o1.stop2stopRoute.depTime < o2.stop2stopRoute.depTime) {
                return -1;
            }
            if (o1.stop2stopRoute.depTime > o2.stop2stopRoute.depTime) {
                return +1;
            }
            if (o1.stop2stopRoute.arrTime < o2.stop2stopRoute.arrTime) {
                return +1; // descending
            }
            if (o1.stop2stopRoute.arrTime > o2.stop2stopRoute.arrTime) {
                return -1; // descending
            }
            return Integer.compare(o1.stop2stopRoute.transfers, o2.stop2stopRoute.transfers);
        });

        List<Integer> dominatedRouteIndices = new ArrayList<>(routes.size());
        for (int route1Index = 0, n = routes.size(); route1Index < n; route1Index++) {
            FoundRoute route1 = routes.get(route1Index);
            if (route1.stop2stopRoute.transfers == 0) {
                // always keep direct routes
                continue;
            }
            for (int route2Index = route1Index + 1; route2Index < n; route2Index++) {
                FoundRoute route2 = routes.get(route2Index);

                if (route2.stop2stopRoute.depTime > route1.stop2stopRoute.arrTime) {
                    // no further route can be contained in route 1
                    break;
                }

                if (route2DominatesRoute1(route2, route1)) {
                    dominatedRouteIndices.add(route1Index);
                    break;
                }
            }
        }

        dominatedRouteIndices.sort((i1, i2) -> Integer.compare(i2, i1)); // reverse sort, descending
        for (Integer routeIndex : dominatedRouteIndices) {
            routes.remove(routeIndex.intValue());
        }
    }

    private static boolean route2DominatesRoute1(FoundRoute route2, FoundRoute route1) {
        boolean isContained = route2.stop2stopRoute.depTime >= route1.stop2stopRoute.depTime && route2.stop2stopRoute.arrTime <= route1.stop2stopRoute.arrTime;
        boolean isStrictlyContained = isContained && (route2.stop2stopRoute.depTime > route1.stop2stopRoute.depTime || route2.stop2stopRoute.arrTime < route1.stop2stopRoute.arrTime);

        boolean equalOrLessTransfers = route2.stop2stopRoute.transfers <= route1.stop2stopRoute.transfers;
        boolean lessTransfers = route2.stop2stopRoute.transfers < route1.stop2stopRoute.transfers;

        boolean equalOrBetterSearchImpedance = route1.searchImpedance >= 1.0 * route2.searchImpedance + 0.0;
        boolean betterSearchImpedance = route1.searchImpedance > 1.0 * route2.searchImpedance + 0.0;

        boolean hasStrictInequality = isStrictlyContained || lessTransfers || betterSearchImpedance;

        return isContained && equalOrLessTransfers && equalOrBetterSearchImpedance && hasStrictInequality;
    }

    /**
     * Sorts the routes in each list of foundRoutes by their departure time.
     */
    public static void sortRoutesByDepartureTime(Map<String, List<FoundRoute>> foundRoutes) {
        for (List<FoundRoute> routes : foundRoutes.values()) {
            routes.sort(UmlegoRouteUtils::compareFoundRoutesByDepartureTime);
        }
    }

    /**
     * Compares two FoundRoute objects by their departure time, travel time without access, and number of transfers.
     */
    public static int compareFoundRoutesByDepartureTime(FoundRoute o1, FoundRoute o2) {
        if (o1.stop2stopRoute.depTime < o2.stop2stopRoute.depTime) {
            return -1;
        }
        if (o1.stop2stopRoute.depTime > o2.stop2stopRoute.depTime) {
            return +1;
        }
        if (o1.stop2stopRoute.travelTimeWithoutAccess < o2.stop2stopRoute.travelTimeWithoutAccess) {
            return -1;
        }
        if (o1.stop2stopRoute.travelTimeWithoutAccess > o2.stop2stopRoute.travelTimeWithoutAccess) {
            return +1;
        }
        return Integer.compare(o1.stop2stopRoute.transfers, o2.stop2stopRoute.transfers);
    }
}
