package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;


/**
 * The {@code WorkerFactory} interface defines a factory for creating workers that process work items in a multi-threaded environment.
 */
public interface WorkflowFactory {

    /**
     * Creates a worker that processes work items from the provided queue.
     */
    AbstractWorker createWorker(BlockingQueue<WorkItem> workerQueue, UmlegoParameters params,
                                List<String> destinationZoneIds, Map<String, List<ZoneConnections.ConnectedStop>> stopsPerZone,
                                Map<String, Map<TransitStopFacility, ZoneConnections.ConnectedStop>> stopLookupPerDestination,
                                DeltaTCalculator deltaTCalculator);


    /**
     * Creates a work item with the specified origin zone.
     * The length of {@link WorkItem#results()} must be equal within all work items and one {@link WorkResultHandler} must be provided for each result type.
     */
    WorkItem createWorkItem(String originZone);

    /**
     * Create a list of result handlers that will be used to process the results of the work items.
     * The length of the list must be equal to {@link WorkItem#results()}.
     */
    List<WorkResultHandler<?>> createResultHandler();

}
