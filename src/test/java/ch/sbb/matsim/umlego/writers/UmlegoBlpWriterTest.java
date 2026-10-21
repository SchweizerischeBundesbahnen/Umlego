package ch.sbb.matsim.umlego.writers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.sbb.matsim.routing.pt.raptor.RaptorRoute;
import ch.sbb.matsim.routing.pt.raptor.RaptorRoute.RoutePart;
import ch.sbb.matsim.umlego.Connectors.ConnectedStop;
import ch.sbb.matsim.umlego.FoundRoute;
import ch.sbb.matsim.umlego.Stop2StopRoute;
import ch.sbb.matsim.umlego.config.CompressionType;
import ch.sbb.matsim.umlego.config.WriterParameters;
import ch.sbb.matsim.umlego.writers.types.volume.TrainNo;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

class UmlegoBlpWriterTest {

    @Test
    void recordsAllLegsOfAChain() throws Exception {
        verifyOccupation(3, 3600, false, 0);
    }

    @Test
    void preservesSingleLegAccounting() throws Exception {
        verifyOccupation(1, 3600, false, 0);
    }

    @Test
    void recordsChainsAcrossMidnightWithoutChangingTimes() throws Exception {
        verifyOccupation(3, 85800, false, 0);
    }

    @Test
    void recordsDestinationAlightingAfterALaterTransfer() throws Exception {
        verifyOccupation(3, 3600, true, 0);
    }

    @Test
    void preservesMinimalDemandThresholdForEveryLeg() throws Exception {
        verifyOccupation(3, 3600, false, 8);
    }

    @Test
    void requiresMatchingDepartureForPartsWithAChainedSuccessor() throws Exception {
        verifyOccupation(3, 3600, false, 0, true);
    }

    @Test
    void requiresMatchingDepartureForASingleLeg() throws Exception {
        verifyOccupation(1, 3600, false, 0, true);
    }

    @Test
    void requiresMatchingDepartureAfterATransfer() throws Exception {
        verifyOccupation(3, 3600, true, 0, true);
    }

    @Test
    void excludesPreviouslyRecordedAlightingDemandFromChainedTerminalVolume() throws Exception {
        verifyOccupation(2, 3600, false, 0, false, 1);
    }

    @Test
    void excludesSubsequentlyRecordedAlightingDemandFromChainedTerminalVolume() throws Exception {
        verifyOccupation(2, 3600, false, 0, false, 2);
    }

    private static void verifyOccupation(int legCount, double startTime, boolean lastIsTransfer, double minimumDemand) throws Exception {
        verifyOccupation(legCount, startTime, lastIsTransfer, minimumDemand, false);
    }

    private static void verifyOccupation(int legCount, double startTime, boolean lastIsTransfer, double minimumDemand,
        boolean repeatedFromStop) throws Exception {
        verifyOccupation(legCount, startTime, lastIsTransfer, minimumDemand, repeatedFromStop, 0);
    }

    private static void verifyOccupation(int legCount, double startTime, boolean lastIsTransfer, double minimumDemand,
        boolean repeatedFromStop, int boundaryAlightingOrder) throws Exception {
        var factory = new TransitScheduleFactoryImpl();
        TransitSchedule schedule = factory.createTransitSchedule();
        List<TransitStopFacility> stops = new ArrayList<>();
        for (int index = 0; index <= legCount; index++) {
            var stop = factory.createTransitStopFacility(Id.create(index, TransitStopFacility.class), new Coord(index, 0), false);
            schedule.addStopFacility(stop);
            stops.add(stop);
        }
        TransitLine line = factory.createTransitLine(Id.create("line", TransitLine.class));
        line.getAttributes().putAttribute("operatorCode", "SBB");
        schedule.addTransitLine(line);
        List<RoutePart> parts = new ArrayList<>();
        List<TrainNo> trains = new ArrayList<>();
        for (int index = 0; index < legCount; index++) {
            var fromStop = stops.get(index);
            var toStop = stops.get(index + 1);
            var routeStops = new ArrayList<>(List.of(factory.createTransitRouteStop(fromStop, 0, 0),
                factory.createTransitRouteStop(toStop, 600, 600)));
            if (repeatedFromStop) {
                routeStops.addFirst(factory.createTransitRouteStop(fromStop, -300, -300));
            }
            var route = factory.createTransitRoute(Id.create(index, TransitRoute.class), null,
                routeStops, "pt");
            line.addRoute(route);
            var departureId = Id.create(index, Departure.class);
            double departureTime = startTime + index * 900;
            route.addDeparture(factory.createDeparture(departureId, departureTime));
            parts.add(part(fromStop, toStop, departureTime, line, route));
            trains.add(new TrainNo("SBB", departureId));
            if (index > 0 && !(lastIsTransfer && index == legCount - 1)) {
                parts.get(index - 1).chainedPart = parts.get(index);
            }
        }
        var routeConstructor = RaptorRoute.class.getDeclaredConstructor(Facility.class, Facility.class, double.class);
        routeConstructor.setAccessible(true);
        RaptorRoute raptorRoute = routeConstructor.newInstance(stops.getFirst(), stops.getLast(), startTime);
        var partsField = RaptorRoute.class.getDeclaredField("parts");
        partsField.setAccessible(true);
        partsField.set(raptorRoute, lastIsTransfer ? List.of(parts.getFirst(), parts.getLast()) : List.of(parts.getFirst()));
        FoundRoute found = new FoundRoute(new Stop2StopRoute(raptorRoute),
            new ConnectedStop("origin", 0, stops.getFirst()), new ConnectedStop("destination", 0, stops.getLast()));
        found.demand = 7;

        UmlegoBlpWriter writer = new UmlegoBlpWriter(null, new WriterParameters(minimumDemand, CompressionType.NONE, Set.of()), schedule);
        if (boundaryAlightingOrder == 1) {
            processBoundaryAlighting(writer, found);
        }
        writer.processRoute("origin", "destination", found);
        if (boundaryAlightingOrder == 2) {
            processBoundaryAlighting(writer, found);
        }

        double expectedDemand = minimumDemand > found.demand ? 0 : found.demand;
        assertEquals(legCount, writer.journeyByTrainNo.size());
        for (int index = 0; index < legCount; index++) {
            var journey = writer.journeyByTrainNo.get(trains.get(index));
            boolean firstInChain = index == 0 || (lastIsTransfer && index == legCount - 1);
            boolean lastInChain = index == legCount - 1 || (lastIsTransfer && index == legCount - 2);
            double boundaryDemand = index == 0 && boundaryAlightingOrder != 0 ? 2 : 0;
            double legDemand = expectedDemand + boundaryDemand;
            var matchingDepartureItem = journey.items().get(repeatedFromStop ? 1 : 0);
            var boardingItem = matchingDepartureItem;
            assertEquals(repeatedFromStop ? 0 : legDemand, journey.items().getFirst().getVolume());
            assertEquals(legDemand, matchingDepartureItem.getVolume());
            assertEquals(lastInChain ? 0 : expectedDemand, journey.items().getLast().getVolume());
            assertEquals(firstInChain ? legDemand : 0, boardingItem.getBoarding());
            if (repeatedFromStop) {
                var otherItem = journey.items().getFirst();
                assertEquals(0, otherItem.getBoarding());
                assertEquals(0, otherItem.getOriginBoarding());
            }
            assertEquals(lastInChain ? expectedDemand : boundaryDemand, journey.items().getLast().getAlighting());
            assertEquals(index == 0 ? legDemand : 0, boardingItem.getOriginBoarding());
            assertEquals(index == legCount - 1 ? expectedDemand : boundaryDemand, journey.items().getLast().getDestinationAlighting());
            assertEquals(startTime + index * 900, matchingDepartureItem.getDeparture());
        }
    }

    private static void processBoundaryAlighting(UmlegoBlpWriter writer, FoundRoute found) {
        var firstPart = found.stop2stopRoute.routeParts.getFirst();
        var successor = firstPart.chainedPart;
        double demand = found.demand;
        try {
            firstPart.chainedPart = null;
            found.demand = 2;
            writer.processRoute("origin", "destination", found);
        } finally {
            firstPart.chainedPart = successor;
            found.demand = demand;
        }
    }

    private static RoutePart part(TransitStopFacility from, TransitStopFacility to, double departure,
        TransitLine line, TransitRoute route) throws Exception {
        var constructor = RoutePart.class.getDeclaredConstructor(TransitStopFacility.class, TransitStopFacility.class,
            String.class, double.class, double.class, double.class, double.class, double.class,
            TransitLine.class, TransitRoute.class, List.class);
        constructor.setAccessible(true);
        return constructor.newInstance(from, to, "pt", departure, departure, departure, departure + 600, 1000, line, route, null);
    }
}