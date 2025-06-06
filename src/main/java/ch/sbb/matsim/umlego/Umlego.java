package ch.sbb.matsim.umlego;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import ch.sbb.matsim.umlego.ZoneConnections.ConnectedStop;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.deltat.IntervalBoundaries;
import ch.sbb.matsim.umlego.demand.UnroutableDemand;
import ch.sbb.matsim.umlego.demand.UnroutableDemandWriter;
import ch.sbb.matsim.umlego.demand.UnroutableDemandWriterFactory;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * @author mrieser / Simunto
 */
public class Umlego {

    private static final Logger LOG = LogManager.getLogger(Umlego.class);

    private final DemandMatrices demand;
    private final Scenario scenario;
    private final Map<String, List<ConnectedStop>> stopsPerZone;
    private DeltaTCalculator deltaTCalculator = new IntervalBoundaries();
    private WorkerFactory workerFactory = UmlegoWorker::new;

    /**
     * List of listeners to be notified about found routes.
     */
    private final List<UmlegoListener> listeners = new LinkedList<>();

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

    /**
     * Adds a listener to be notified about found routes.
     *
     * @param listener the listener to be added
     */
    public void addListener(UmlegoListener listener) {
        if (this.listeners.contains(listener)) {
            throw new IllegalArgumentException("Listener " + listener.getClass().getName() + " already registered.");
        }

        this.listeners.add(listener);
    }

    /**
     * Sets the {@link DeltaTCalculator} to be used for calculating adaptation times.
     */
    public Umlego setDeltaTCalculator(DeltaTCalculator deltaTCalculator) {
        this.deltaTCalculator = deltaTCalculator;
        return this;
    }

    /**
     * Sets the {@link WorkerFactory} to be used for creating worker threads.
     */
    public Umlego setWorkerFactory(WorkerFactory workerFactory) {
        this.workerFactory = workerFactory;
        return this;
    }

    /**
     * Retrieves the listener of the specified type from the collection of registered listeners.
     *
     * @param <T>   the type of the listener to retrieve, which must extend {@code UmlegoListener}
     * @param type  the {@code Class} object representing the type of listener to search for
     * @return an instance of the listener of the specified type
     * @throws IllegalArgumentException if no listener of the specified type is found
     */
    public <T extends UmlegoListener> T getListener(Class<T> type) {
        for (UmlegoListener listener : this.listeners) {
            if (type.isInstance(listener)) {
                return type.cast(listener);
            }
        }
        throw new IllegalArgumentException("No listener of type " + type.getName() + " found.");
    }

    public void run(UmlegoParameters params, int threadCount, String outputFolder) throws ZoneNotFoundException {
        // TODO ?: Why null values and not just delete the param on the run method?
        run(null, null, params, threadCount, outputFolder);
    }

    public void run(List<String> originZones, List<String> destinationZones, UmlegoParameters params, int threadCount,
        String outputFolder) throws ZoneNotFoundException {
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

        // Default listeners which are always added
        addListener(new UmlegoSkimCalculator());

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
        UmlegoWorkItem workEndMarker = new UmlegoWorkItem(null, null);
        CompletableFuture<WorkResult> writeEndMarker = new CompletableFuture<>();
        writeEndMarker.complete(new WorkResult(null, null, null));

        BlockingQueue<WorkItem> workerQueue = new LinkedBlockingQueue<>(5 * threadCount);
        BlockingQueue<Future<WorkResult>> writerQueue = new LinkedBlockingQueue<>(4 * threadCount);

        // start worker threads
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threads.length; i++) {
            SwissRailRaptor raptor = new SwissRailRaptor.Builder(raptorData, this.scenario.getConfig()).build();

            AbstractWorker worker = workerFactory.createWorker(
                    workerQueue, params, this.demand, raptor, raptorParams, destinationZoneIds,
                    this.stopsPerZone, stopLookupPerDestination, this.deltaTCalculator);

            threads[i] = new Thread(worker);
            threads[i].start();
        }

        // start writer threads
        UmlegoResultWorker writerManager = new UmlegoResultWorker(writerQueue, outputFolder, originZoneIds,
                destinationZoneIds, listeners, scenario.getTransitSchedule(), params.writer());
        new Thread(writerManager).start();

        // submit work items into queues
        for (String originZoneId : originZoneIds) {

            try {
                WorkItem workItem = workerFactory.createWorkItem(originZoneId);
                writerQueue.put(workItem.result());
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

}
