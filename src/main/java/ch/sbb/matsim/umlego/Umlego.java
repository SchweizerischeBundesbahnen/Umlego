package ch.sbb.matsim.umlego;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorRoute;
import ch.sbb.matsim.routing.pt.raptor.RaptorRoute.RoutePart;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import ch.sbb.matsim.umlego.UmlegoWorker.WorkItem;
import ch.sbb.matsim.umlego.UmlegoWorker.WorkResult;
import ch.sbb.matsim.umlego.ZoneConnections.ConnectedStop;
import ch.sbb.matsim.umlego.config.UmlegoWriterType;
import ch.sbb.matsim.umlego.demand.UnroutableDemand;
import ch.sbb.matsim.umlego.demand.UnroutableDemandWriter;
import ch.sbb.matsim.umlego.demand.UnroutableDemandWriterFactory;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.writers.UmlegoListenerInterface;
import ch.sbb.matsim.umlego.writers.UmlegoWriter;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser / Simunto
 */
public class Umlego {

    private static final Logger LOG = LogManager.getLogger(Umlego.class);

    private final DemandMatrices demand;
    private final Scenario scenario;
    private final Map<String, List<ConnectedStop>> stopsPerZone;

    /**
     * Constructor.
     *
     * @param demand demand matrices
     * @param scenario MATSim object collecting all sort of data
     * @param stopsPerZone stop per zone
     */
    public Umlego(DemandMatrices demand, Scenario scenario, Map<String, List<ConnectedStop>> stopsPerZone) {
        this.demand = demand;
        this.scenario = scenario;
        this.stopsPerZone = stopsPerZone;
    }

    public void run(UmlegoParameters params, int threadCount, String outputFolder, LocalDate targetDate) throws ZoneNotFoundException {
        // TODO ?: Why null values and not just delete the param on the run method?
        run(null, null, params, threadCount, outputFolder, targetDate);
    }

    public void run(List<String> originZones, List<String> destinationZones, UmlegoParameters params, int threadCount,
        String outputFolder, LocalDate targetDate) throws ZoneNotFoundException {
        List<String> originZoneIds = originZones == null ? new ArrayList<>(demand.getLookup().getAllLookupValues())
            : new ArrayList<>(originZones);
        originZoneIds.sort(String::compareTo);
        List<String> destinationZoneIds =
            destinationZones == null ? new ArrayList<>(demand.getLookup().getAllLookupValues())
                : new ArrayList<>(destinationZones);
        destinationZoneIds.sort(String::compareTo);

        // detect relevant stops
        List<ConnectedStop> emptyList = Collections.emptyList();
        IntSet destinationStopIndices = new IntOpenHashSet();
        for (String zoneId : destinationZoneIds) {
            List<TransitStopFacility> stops = this.stopsPerZone.getOrDefault(zoneId, emptyList).stream()
                .map(ConnectedStop::stopFacility).toList();
            for (TransitStopFacility stop : stops) {
                destinationStopIndices.add(stop.getId().index());
            }
        }
        LOG.info("Detected {} stops as potential destinations", destinationStopIndices.size());

        Map<String, Map<TransitStopFacility, ConnectedStop>> stopLookupPerDestination = new HashMap<>();
        for (String destinationZoneId : destinationZoneIds) {
            List<ConnectedStop> stopsPerDestinationZone = this.stopsPerZone.getOrDefault(destinationZoneId, emptyList);
            Map<TransitStopFacility, ConnectedStop> destinationStopLookup = new HashMap<>();
            for (ConnectedStop stop : stopsPerDestinationZone) {
                destinationStopLookup.put(stop.stopFacility(), stop);
            }
            stopLookupPerDestination.put(destinationZoneId, destinationStopLookup);
        }

        // prepare SwissRailRaptor
        // TODO: these parameters could be added to a central location.
        RaptorParameters raptorParams = RaptorUtils.createParameters(scenario.getConfig());
        raptorParams.setTransferPenaltyFixCostPerTransfer(0.01);
        raptorParams.setTransferPenaltyMinimum(0.01);
        raptorParams.setTransferPenaltyMaximum(0.01);

        RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(scenario.getConfig());
        raptorConfig.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
        // make sure SwissRailRaptor does not add any more transfers than what is specified in minimal transfer times:
        raptorConfig.setBeelineWalkConnectionDistance(10.0);
        SwissRailRaptorData raptorData = SwissRailRaptorData.create(this.scenario.getTransitSchedule(),
            this.scenario.getTransitVehicles(), raptorConfig, this.scenario.getNetwork(), null);

        // prepare queues with work items
		/* Writing might actually be slower than the computation, resulting in more and more
		   memory being used for the found routes until they get written. To prevent
		   OutOfMemoryErrors, we use a blocking queue for the writer with a limited capacity.
		 */
        WorkItem workEndMarker = new WorkItem(null, null);
        CompletableFuture<WorkResult> writeEndMarker = new CompletableFuture<>();
        writeEndMarker.complete(new WorkResult(null, null, null));

        BlockingQueue<WorkItem> workerQueue = new LinkedBlockingQueue<>(5 * threadCount);
        BlockingQueue<Future<WorkResult>> writerQueue = new LinkedBlockingQueue<>(4 * threadCount);

        // start worker threads
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threads.length; i++) {
            SwissRailRaptor raptor = new SwissRailRaptor.Builder(raptorData, this.scenario.getConfig()).build();
            threads[i] = new Thread(
                new UmlegoWorker(workerQueue, params, this.demand, raptor, raptorParams, destinationZoneIds,
                    this.stopsPerZone, stopLookupPerDestination));
            threads[i].start();
        }

        // start writer threads

        UmlegoWriter writerManager = new UmlegoWriter(writerQueue, outputFolder, originZoneIds, destinationZoneIds,
            params.writer, targetDate);
        new Thread(writerManager).start();

        // submit work items into queues
        for (String originZoneId : originZoneIds) {
            try {
                CompletableFuture<UmlegoWorker.WorkResult> future = new CompletableFuture<>();
                UmlegoWorker.WorkItem workItem = new UmlegoWorker.WorkItem(originZoneId, future);
                writerQueue.put(future);
                workerQueue.put(workItem);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        // once all zones are submitted for calculation, add the end-markers to the queues
        try {
            for (int i = 0; i < threadCount; i++) {
                workerQueue.put(workEndMarker);
            }
            writerQueue.put(writeEndMarker);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        UnroutableDemand unroutableDemand = writerManager.getUnroutableDemand();
        UnroutableDemandWriter demandWriter = UnroutableDemandWriterFactory.createWriter(outputFolder);
        demandWriter.write(unroutableDemand);
    }

    public record SearchImpedanceParameters(
        double betaInVehicleTime,
        double betaAccessTime,
        double betaEgressTime,
        double betaWalkTime,
        double betaTransferWaitTime,
        double betaTransferCount
    ) {

    }

    public record PreselectionParameters(
        double betaMinImpedance,
        double constImpedance
    ) {

    }

    public record PerceivedJourneyTimeParameters(
        double betaInVehicleTime,
        double betaAccessTime,
        double betaEgressTime,
        double betaWalkTime,
        double betaTransferWaitTime,
        double transferFix,
        double transferTraveltimeFactor,
        double secondsPerAdditionalStop
    ) {

    }

    public record RouteImpedanceParameters(
        double betaPerceivedJourneyTime,
        double betaDeltaTEarly,
        double betaDeltaTLate
    ) {

    }

    public record RouteSelectionParameters(
        boolean limitSelectionToTimewindow,
        double beforeTimewindow,
        double afterTimewindow,
        RouteUtilityCalculator utilityCalculator
    ) {

    }

    public record WriterParameters(
        double minimalDemandForWriting,
        Set<UmlegoWriterType> writerTypes,
        TransitSchedule schedule,
        List<UmlegoListenerInterface> listeners
    ) {

    }

    public record UmlegoParameters(
        int maxTransfers,
        SearchImpedanceParameters search,
        PreselectionParameters preselection,
        PerceivedJourneyTimeParameters pjt,
        RouteImpedanceParameters impedance,
        RouteSelectionParameters routeSelection,
        WriterParameters writer
    ) {

    }

    public static class FoundRoute {

        public final TransitStopFacility originStop;
        public final TransitStopFacility destinationStop;
        public ConnectedStop originConnectedStop;
        public ConnectedStop destinationConnectedStop;
        public double depTime;
        public double arrTime;
        public final double travelTimeWithoutAccess;
        public double travelTimeWithAccess = Double.NaN;
        public int transfers;
        public final double distance;
        public List<RaptorRoute.RoutePart> routeParts = new ArrayList<>();
        public double searchImpedance = Double.NaN; // Suchwiderstand
        public double perceivedJourneyTimeMin = Double.NaN; // Empfundene Reisezeit
        public Object2DoubleMap<String> demand = new Object2DoubleOpenHashMap<>();
        public Object2DoubleMap<String> adaptationTime = new Object2DoubleOpenHashMap<>();
        public double originality = 0; // Eigenständigkeit

        public FoundRoute(RaptorRoute route) {
            double firstDepTime = Double.NaN;
            double lastArrTime = Double.NaN;

            TransitStopFacility originStopFacility = null;
            TransitStopFacility destinationStopFacility = null;

            double distanceSum = 0;
            RaptorRoute.RoutePart prevTransfer = null;
            int stageCount = 0;
            for (RaptorRoute.RoutePart part : route.getParts()) {
                if (part.line == null) {
                    // it is a transfer
                    prevTransfer = part;
                    // still update the destination stop in case we arrive the destination by a transfer / walk-link
                    destinationStopFacility = part.toStop;
                } else {
                    stageCount++;
                    if (routeParts.isEmpty()) {
                        // it is the first real stage
                        firstDepTime = part.vehicleDepTime;
                        originStopFacility = part.fromStop;
                    } else if (prevTransfer != null) {
                        this.routeParts.add(prevTransfer);
                    }
                    this.routeParts.add(part);
                    lastArrTime = part.arrivalTime;
                    destinationStopFacility = part.toStop;
                    distanceSum += part.distance;
                }
            }
            this.originStop = originStopFacility;
            this.destinationStop = destinationStopFacility;
            this.depTime = firstDepTime;
            this.arrTime = lastArrTime;
            this.travelTimeWithoutAccess = this.arrTime - this.depTime;
            this.transfers = stageCount - 1;
            this.distance = distanceSum;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            FoundRoute that = (FoundRoute) o;
            boolean isEqual = Double.compare(depTime, that.depTime) == 0
                && Double.compare(arrTime, that.arrTime) == 0
                && transfers == that.transfers
                && Objects.equals(originStop.getId(), that.originStop.getId())
                && Objects.equals(destinationStop.getId(), that.destinationStop.getId());
            if (isEqual) {
                // also check route parts
                for (int i = 0; i < routeParts.size(); i++) {
                    RaptorRoute.RoutePart routePartThis = this.routeParts.get(i);
                    RaptorRoute.RoutePart routePartThat = that.routeParts.get(i);

                    boolean partIsEqual =
                        ((routePartThis.line == null && routePartThat.line == null) || (routePartThis.line != null
                            && routePartThat.line != null && Objects.equals(routePartThis.line.getId(),
                            routePartThat.line.getId())))
                            && ((routePartThis.route == null && routePartThat.route == null) || (
                            routePartThis.route != null && routePartThat.route != null && Objects.equals(
                                routePartThis.route.getId(), routePartThat.route.getId())));
                    if (!partIsEqual) {
                        return false;
                    }
                }
            }
            return isEqual;
        }

        @Override
        public int hashCode() {
            return Objects.hash(depTime, arrTime, transfers);
        }

        public String getRouteAsString() {
            StringBuilder details = new StringBuilder();
            for (RaptorRoute.RoutePart part : this.routeParts) {
                if (part.line == null) {
                    continue;
                }
                if (!details.isEmpty()) {
                    details.append(", ");
                }
                details.append(getPartString(part));
                while (part.chainedPart != null) {
                    part = part.chainedPart;
                    details.append(" => ");
                    details.append(getPartString(part));
                }
            }
            return details.toString();
        }

        private String getPartString(RoutePart part) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(part.line.getId());
            stringBuilder.append(" (");
            stringBuilder.append(part.route.getId());
            stringBuilder.append(") ");
            stringBuilder.append(": ");
            stringBuilder.append(part.fromStop.getName());
            stringBuilder.append(' ');
            stringBuilder.append(Time.writeTime(part.vehicleDepTime));
            stringBuilder.append(" - ");
            stringBuilder.append(part.toStop.getName());
            stringBuilder.append(' ');
            stringBuilder.append(Time.writeTime(part.arrivalTime));
            return stringBuilder.toString();
        }
    }
}
