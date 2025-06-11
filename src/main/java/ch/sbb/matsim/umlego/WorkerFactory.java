package ch.sbb.matsim.umlego;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;


/**
 * The {@code WorkerFactory} interface defines a factory for creating workers that process work items in a multi-threaded environment.
 */
public interface WorkerFactory {

    /**
     * Creates a worker that processes work items from the provided queue.
     */
    AbstractWorker createWorker(BlockingQueue<WorkItem> workerQueue, UmlegoParameters params, DemandMatrices demand,
                                SwissRailRaptor raptor, RaptorParameters raptorParams, List<String> destinationZoneIds,
                                Map<String, List<ZoneConnections.ConnectedStop>> stopsPerZone,
                                Map<String, Map<TransitStopFacility, ZoneConnections.ConnectedStop>> stopLookupPerDestination,
                                DeltaTCalculator deltaTCalculator);


    /**
     * Creates a work item with the specified origin zone.
     */
    default WorkItem createWorkItem(String originZone) {
        CompletableFuture<WorkResult> future = new CompletableFuture<>();
        return new UmlegoWorkItem(originZone, List.of(future));
    }

}
