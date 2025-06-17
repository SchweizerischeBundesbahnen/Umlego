package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.config.PerceivedJourneyTimeParameters;
import ch.sbb.matsim.umlego.config.SearchImpedanceParameters;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.demand.UnroutableDemand;
import ch.sbb.matsim.umlego.demand.UnroutableDemandPart;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorRoute;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.umlego.ZoneConnections.ConnectedStop;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * @author mrieser / Simunto
 */
public class UmlegoWorker extends AbstractWorker {

    private final UmlegoParameters params;
    private final DemandMatrices demand;
    private final List<String> demandMatrixNames;
    private final SwissRailRaptor raptor;
    private final RaptorParameters raptorParams;
    private final List<String> destinationZoneIds;
    private final Map<String, List<ConnectedStop>> stopsPerZone;
    private final Map<String, Map<TransitStopFacility, ConnectedStop>> stopLookupPerDestination;
    private final RouteUtilityCalculator utilityCalculator;
    private final DeltaTCalculator deltaTCalculator;

    public UmlegoWorker(BlockingQueue<WorkItem> workerQueue,
                        UmlegoParameters params,
                        DemandMatrices demand,
                        SwissRailRaptor raptor,
                        RaptorParameters raptorParams,
                        List<String> destinationZoneIds,
                        Map<String, List<ConnectedStop>> stopsPerZone,
                        Map<String, Map<TransitStopFacility, ConnectedStop>> stopLookupPerDestination,
                        DeltaTCalculator deltaTCalculator) {
        super(workerQueue);
        this.params = params;
        this.demand = demand;
        this.demandMatrixNames = this.demand.getMatrixNames();
        this.raptor = raptor;
        this.raptorParams = raptorParams;
        this.destinationZoneIds = destinationZoneIds;
        this.stopsPerZone = stopsPerZone;
        this.stopLookupPerDestination = stopLookupPerDestination;
        this.utilityCalculator = params.routeSelection().utilityCalculator().createUtilityCalculator();
        this.deltaTCalculator = deltaTCalculator;
    }

    private static void calculateOriginality(List<FoundRoute> routes) {
        routes.sort(UmlegoRouteUtils::compareFoundRoutesByDepartureTime);
        for (int i = 0; i < routes.size(); i++) {
            FoundRoute route1 = routes.get(i);
            int countEqualRoutes = 1; // comparison with itself would always result in equality

            // search for equal routes before this route
            for (int j = i - 1; j >= 0; j--) {
                FoundRoute route2 = routes.get(j);
                if (route1.stop2stopRoute.depTime != route2.stop2stopRoute.depTime) {
                    break; // because the routes are sorted, there cannot be any more equal routes
                }

                boolean areEqual = (route1.stop2stopRoute.depTime == route2.stop2stopRoute.depTime)
                        && (route1.stop2stopRoute.arrTime == route2.stop2stopRoute.arrTime)
                        && (route1.searchImpedance == route2.searchImpedance)
                        && route1.stop2stopRoute.transfers == route2.stop2stopRoute.transfers;
                if (areEqual) {
                    countEqualRoutes++;
                }
            }
            // search for equal routes after this route
            for (int j = i + 1; j < routes.size(); j++) {
                FoundRoute route2 = routes.get(j);
                if (route1.stop2stopRoute.depTime != route2.stop2stopRoute.depTime) {
                    break; // because the routes are sorted, there cannot be any more equal routes
                }

                boolean areEqual = (route1.stop2stopRoute.depTime == route2.stop2stopRoute.depTime)
                        && (route1.stop2stopRoute.arrTime == route2.stop2stopRoute.arrTime)
                        && (route1.searchImpedance == route2.searchImpedance)
                        && route1.stop2stopRoute.transfers == route2.stop2stopRoute.transfers;
                if (areEqual) {
                    countEqualRoutes++;
                }
            }
            route1.originality = 1.0 / countEqualRoutes;
        }
    }

    @Override
    protected UmlegoWorkResult processOriginZone(WorkItem workItem) throws ZoneNotFoundException {
        Map<String, List<FoundRoute>> foundRoutes = calculateRoutesForZone(workItem.originZone());
        calculateRouteCharacteristics(foundRoutes);
        filterRoutes(foundRoutes);
        calculateOriginality(foundRoutes);
        return assignDemand(workItem.originZone(), foundRoutes);
    }

    protected final Map<String, List<FoundRoute>> calculateRoutesForZone(String originZone) throws ZoneNotFoundException {
        IntSet activeDestinationStopIndices = getActiveDestinationStopIndices(originZone);
        Map<TransitStopFacility, Map<TransitStopFacility, Map<Stop2StopRoute, Boolean>>> foundRoutes = new HashMap<>();
        for (ConnectedStop stop : stopsPerZone.getOrDefault(originZone, Collections.emptyList())) {
            calcRoutesFromStop(stop.stopFacility(), activeDestinationStopIndices, foundRoutes);
        }
        return aggregateOnZoneLevel(originZone, foundRoutes);
    }

    private IntSet getActiveDestinationStopIndices(String originZone) throws ZoneNotFoundException {
        List<ConnectedStop> emptyList = Collections.emptyList();
        IntSet destinationStopIndices = new IntOpenHashSet();

        for (String destinationZone : this.destinationZoneIds) {
            // exclude intrazonal demand
            if (!destinationZone.equals(originZone)) {
                for (String matrixName : this.demandMatrixNames) {
                    double value = this.demand.getMatrixValue(originZone, destinationZone, matrixName);
                    if (value > 0) {
                        List<TransitStopFacility> stops = this.stopsPerZone.getOrDefault(destinationZone, emptyList).stream().map(stop -> stop.stopFacility()).toList();
                        for (TransitStopFacility stop : stops) {
                            destinationStopIndices.add(stop.getId().index());
                        }
                        break;
                    }
                }
            }
        }
        return destinationStopIndices;
    }

    private void calcRoutesFromStop(TransitStopFacility originStop, IntSet destinationStopIndices, Map<TransitStopFacility, Map<TransitStopFacility, Map<Stop2StopRoute, Boolean>>> foundRoutes) {
        this.raptorParams.setMaxTransfers(this.params.maxTransfers());
        this.raptor.calcTreesObservable(
                originStop,
                0,
                Double.POSITIVE_INFINITY,
                this.raptorParams,
                null,
                (departureTime, arrivalStop, arrivalTime, transferCount, route) -> {
                    if (destinationStopIndices.contains(arrivalStop.getId().index())) {
                        Stop2StopRoute stop2stopRoute = new Stop2StopRoute(route.get());
                        if (stop2stopRoute.originStop != null) {
                            foundRoutes
                                    .computeIfAbsent(stop2stopRoute.originStop, stop -> new HashMap<>())
                                    .computeIfAbsent(stop2stopRoute.destinationStop, stop -> new HashMap<>())
                                    .put(stop2stopRoute, Boolean.TRUE);
                        }
                    }
                });
    }

    /**
     * Creates a Map containing for each destination zone id the List of found routes,
     * leading from the originZoneId to the destination, over the whole day.
     */
    private Map<String, List<FoundRoute>> aggregateOnZoneLevel(String originZoneId, Map<TransitStopFacility, Map<TransitStopFacility, Map<Stop2StopRoute, Boolean>>> foundRoutesPerStop) {
        List<ConnectedStop> emptyList = Collections.emptyList();
        Map<String, List<FoundRoute>> foundRoutesPerZone = new HashMap<>();

        List<ConnectedStop> stopsPerOriginZone = this.stopsPerZone.getOrDefault(originZoneId, emptyList);
        Map<TransitStopFacility, ConnectedStop> originStopLookup = new HashMap<>();
        for (ConnectedStop stop : stopsPerOriginZone) {
            originStopLookup.put(stop.stopFacility(), stop);
        }

        for (String destinationZoneId : destinationZoneIds) {
            Map<TransitStopFacility, ConnectedStop> destinationStopLookup = this.stopLookupPerDestination.get(destinationZoneId);
            List<FoundRoute> allRoutesFromTo = new ArrayList<>();
            for (ConnectedStop originStop : stopsPerOriginZone) {
                Map<TransitStopFacility, Map<Stop2StopRoute, Boolean>> routesPerDestinationStop = foundRoutesPerStop.get(originStop.stopFacility());
                if (routesPerDestinationStop != null) {
                    for (ConnectedStop destinationStop : this.stopsPerZone.getOrDefault(destinationZoneId, emptyList)) {
                        Map<Stop2StopRoute, Boolean> routesPerOriginDestinationStop = routesPerDestinationStop.get(destinationStop.stopFacility());
                        if (routesPerOriginDestinationStop != null) {
                            for (Stop2StopRoute route : routesPerOriginDestinationStop.keySet()) {
                                ConnectedStop originConnectedStop = originStopLookup.get(route.originStop);
                                ConnectedStop destinationConnectedStop = destinationStopLookup.get(route.destinationStop);

                                boolean invalidRoute =
                                        UmlegoRouteUtils.routeStartsWithTransferWithinSameZone(route, originStopLookup.keySet())
                                                || UmlegoRouteUtils.routeEndsWithTransferWithinSameZone(route, destinationStopLookup.keySet());

                                if (originConnectedStop != null && destinationConnectedStop != null && !invalidRoute) {
                                    // otherwise the route would not be valid, e.g. due to an additional transfer at the start or end
                                    FoundRoute foundRoute = new FoundRoute(route, originConnectedStop, destinationConnectedStop);
                                    allRoutesFromTo.add(foundRoute);
                                }
                            }
                        }
                    }
                }
            }
            foundRoutesPerZone.put(destinationZoneId, allRoutesFromTo);
        }
        return foundRoutesPerZone;
    }

    protected final void calculateOriginality(Map<String, List<FoundRoute>> foundRoutes) {
        for (List<FoundRoute> routes : foundRoutes.values()) {
            calculateOriginality(routes);
        }
    }

    protected final void filterRoutes(Map<String, List<FoundRoute>> foundRoutes) {
        for (List<FoundRoute> routes : foundRoutes.values()) {
            filterRoutes(routes);
        }
    }

    private void filterRoutes(List<FoundRoute> routes) {
        UmlegoRouteUtils.removeDominatedRoutes(routes);
        preselectRoute(routes);
    }

    protected final void preselectRoute(List<FoundRoute> routes) {
        int minTransfers = Integer.MAX_VALUE;
        double minSearchImpedance = Double.POSITIVE_INFINITY;
        double minTraveltime = Double.POSITIVE_INFINITY;
        for (FoundRoute route : routes) {
            if (route.stop2stopRoute.transfers < minTransfers) {
                minTransfers = route.stop2stopRoute.transfers;
            }
            if (route.searchImpedance < minSearchImpedance) {
                minSearchImpedance = route.searchImpedance;
            }
            if (route.travelTimeWithAccess < minTraveltime) {
                minTraveltime = route.travelTimeWithAccess;
            }
        }

        ListIterator<FoundRoute> it = routes.listIterator();
        while (it.hasNext()) {
            FoundRoute route = it.next();
            if ((route.searchImpedance > (this.params.preselection().betaMinImpedance() * minSearchImpedance + this.params.preselection().constImpedance()))
                    || (route.stop2stopRoute.transfers > (minTransfers + 3) && (route.travelTimeWithAccess > minTraveltime))
            ) {
                it.remove();
            }
        }
    }

    protected final void calculateRouteCharacteristics(Map<String, List<FoundRoute>> foundRoutes) {
        for (List<FoundRoute> routes : foundRoutes.values()) {
            for (FoundRoute route : routes) {
                calculateRouteCharacteristics(route);
            }
        }
    }

    private void calculateRouteCharacteristics(FoundRoute route) {
        double inVehicleTime = 0;
        double accessTime = route.originConnectedStop.walkTime();
        double egressTime = route.destinationConnectedStop.walkTime();
        double walkTime = 0;
        double transferWaitTime = 0;
        double transferCount = route.stop2stopRoute.transfers;

        boolean hadTransferBefore = false;
        int additionalStopCount = 0;
        for (RaptorRoute.RoutePart part : route.stop2stopRoute.routeParts) {
            if (part.line == null) {
                // it is a transfer
                walkTime += (part.arrivalTime - part.depTime);
                hadTransferBefore = true;
            } else {
                if (hadTransferBefore) {
                    transferWaitTime += (part.vehicleDepTime - part.depTime);
                }
                inVehicleTime += (part.arrivalTime - part.vehicleDepTime);
                hadTransferBefore = false;
                int startIndex = -1;
                int endIndex = -1;
                List<TransitRouteStop> stops = part.route.getStops();
                for (int i = 0; i < stops.size(); i++) {
                    TransitRouteStop routeStop = stops.get(i);
                    if (routeStop.getStopFacility().getId().equals(part.toStop.getId()) && startIndex >= 0) {
                        endIndex = i;
                        break;
                    }
                    if (routeStop.getStopFacility().getId().equals(part.fromStop.getId())) {
                        startIndex = i;
                    }
                }
                if (startIndex >= 0 && endIndex >= 0) {
                    additionalStopCount += (endIndex - startIndex - 1);
                }
            }
        }

        double expectedTotalTime = route.stop2stopRoute.routeParts.get(route.stop2stopRoute.routeParts.size() - 1).arrivalTime - route.stop2stopRoute.routeParts.get(0).vehicleDepTime;
        if ((walkTime + transferWaitTime + inVehicleTime) != expectedTotalTime) {
            System.err.println("INCONSISTENT TIMES " + route.stop2stopRoute.getRouteAsString());
        }
        double totalTravelTime = expectedTotalTime + accessTime + egressTime;

        PerceivedJourneyTimeParameters pjtParams = this.params.pjt();
        route.perceivedJourneyTimeMin = pjtParams.betaInVehicleTime() * (inVehicleTime / 60.0)
                + pjtParams.betaAccessTime() * (accessTime / 60.0)
                + pjtParams.betaEgressTime() * (egressTime / 60.0)
                + pjtParams.betaWalkTime() * (walkTime / 60.0)
                + pjtParams.betaTransferWaitTime() * (transferWaitTime / 60.0)
                + transferCount * (pjtParams.transferFix() + pjtParams.transferTraveltimeFactor() * (totalTravelTime / 60.0))
                + (pjtParams.secondsPerAdditionalStop() / 60.0) * additionalStopCount;

        SearchImpedanceParameters searchParams = this.params.search();
        route.searchImpedance =
                searchParams.betaInVehicleTime() * (inVehicleTime / 60.0)
                        + searchParams.betaAccessTime() * (accessTime / 60.0)
                        + searchParams.betaEgressTime() * (egressTime / 60.0)
                        + searchParams.betaWalkTime() * (walkTime / 60.0)
                        + searchParams.betaTransferWaitTime() * (transferWaitTime / 60.0)
                        + searchParams.betaTransferCount() * transferCount;
    }

    protected final UmlegoWorkResult assignDemand(String originZone, Map<String, List<FoundRoute>> foundRoutes) throws ZoneNotFoundException {
        UmlegoRouteUtils.sortRoutesByDepartureTime(foundRoutes);
        UnroutableDemand unroutableDemand = new UnroutableDemand();
        for (String destinationZone : this.destinationZoneIds) {
            var routes = foundRoutes.get(destinationZone);
            if (routes == null || routes.isEmpty()) {
                double sum = 0;
                for (String matrixName : this.demandMatrixNames) {
                    double value = this.demand.getMatrixValue(originZone, destinationZone, matrixName);
                    sum += value;
                }
                if (sum > 0) {
                    unroutableDemand.addPart(new UnroutableDemandPart(originZone, destinationZone, sum));
                }
            } else {
                for (String matrixName : this.demandMatrixNames) {
                    double value = this.demand.getMatrixValue(originZone, destinationZone, matrixName);
                    if (value > 0) {
                        int matrixNumber = Integer.parseInt(matrixName);
                        double startTime = (matrixNumber - 1) * 10 * 60; // 10-minute time slots, in seconds TODO: make configurable
                        double endTime = (matrixNumber) * 10 * 60;
                        assignDemand(originZone, destinationZone, startTime, endTime, value, routes, unroutableDemand);
                    }
                }
            }
        }
        return new UmlegoWorkResult(originZone, foundRoutes, unroutableDemand);
    }

    private void assignDemand(String originZone, String destinationZone, double startTime, double endTime, double odDemand, List<FoundRoute> routes, UnroutableDemand unroutableDemand) {
        FoundRoute[] potentialRoutes;
        boolean limit = this.params.routeSelection().limitSelectionToTimewindow();
        if (limit) {
            double earliestDeparture = startTime - this.params.routeSelection().beforeTimewindow();
            double latestDeparture = endTime + this.params.routeSelection().afterTimewindow();
            potentialRoutes = routes.stream().filter(route -> ((route.stop2stopRoute.depTime - route.originConnectedStop.walkTime()) >= earliestDeparture)
                    && ((route.stop2stopRoute.depTime - route.originConnectedStop.walkTime() <= latestDeparture))).toList().toArray(new FoundRoute[0]);
        } else {
            potentialRoutes = routes.toArray(new FoundRoute[0]);
        }
        if (potentialRoutes.length == 0) {
            unroutableDemand.addPart(new UnroutableDemandPart(originZone, destinationZone, odDemand));
            return;
        }
        double timeWindow = endTime - startTime;
        double stepSize = 60.0; // sample every minute
        int samples = (int) (timeWindow / stepSize);
        double sharePerSample = 1.0 / ((double) samples);
        double[] impedances = new double[potentialRoutes.length];
        double[] deltas = new double[potentialRoutes.length];
        double minImpedance = Double.POSITIVE_INFINITY;
        double[] routeUtilities = new double[potentialRoutes.length];
        double betaPJT = this.params.impedance().betaPerceivedJourneyTime();
        double betaDeltaTEarly = this.params.impedance().betaDeltaTEarly();
        double betaDeltaTLate = this.params.impedance().betaDeltaTLate();

        for (int sample = 0; sample < samples; sample++) {
            double time = startTime + sample * stepSize;
            double utilitiesSum = 0;
            for (int i = 0; i < potentialRoutes.length; i++) {
                FoundRoute route = potentialRoutes[i];
                double routeDepTime = route.stop2stopRoute.depTime - route.originConnectedStop.walkTime();

                double deltaTEarly = this.deltaTCalculator.calculateDeltaTEarly(routeDepTime, time, time + stepSize);
                double deltaTLate = this.deltaTCalculator.calculateDeltaTLate(routeDepTime, time, time + stepSize);

                // one of both must be zero, so we can sum them up
                deltas[i] = Math.abs(deltaTEarly + deltaTLate);
                double impedance = betaPJT * route.perceivedJourneyTimeMin
                        + betaDeltaTEarly * (deltaTEarly / 60.0) + betaDeltaTLate * (deltaTLate / 60.0);
                impedances[i] = impedance;
                if (impedance < minImpedance) {
                    minImpedance = impedance;
                }
            }
            for (int i = 0; i < potentialRoutes.length; i++) {
                double impedance = impedances[i];
                double utility = utilityCalculator.calculateUtility(impedance, minImpedance);
                routeUtilities[i] = utility * potentialRoutes[i].originality;
                utilitiesSum += routeUtilities[i];
            }
            for (int i = 0; i < potentialRoutes.length; i++) {
                double delta = deltas[i];
                double routeShare = routeUtilities[i] / utilitiesSum;
                double routeDemand = odDemand * sharePerSample * routeShare;
                FoundRoute route = potentialRoutes[i];
                route.demand += routeDemand;
                route.adaptationTime += delta * routeDemand;
            }
        }
    }

}
