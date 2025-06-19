package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;


/**
 * The {@code WorkerFactory} interface defines a factory for creating workers that process work items in a multi-threaded environment.
 *
 * @param <T> the type of work item that the factory creates
 */
public interface WorkflowFactory<T extends WorkItem> {

    /**
     * Creates a worker that processes work items from the provided queue.
     */
    AbstractWorker<T> createWorker(BlockingQueue<T> workerQueue, UmlegoParameters params,
                                List<String> destinationZoneIds, Map<String, List<ZoneConnections.ConnectedStop>> stopsPerZone,
                                Map<String, Map<TransitStopFacility, ZoneConnections.ConnectedStop>> stopLookupPerDestination,
                                DeltaTCalculator deltaTCalculator);


    /**
     * Creates a work item with the specified origin zone.
     * The length of {@link WorkItem#results()} must be equal within all work items and one {@link WorkResultHandler} must be provided for each result type.
     */
    T createWorkItem(String originZone);

    /**
     * Create a list of result handlers that will be used to process the results of the work items.
     * The length of the list must be equal to {@link WorkItem#results()}.
     *
     * @param params             the parameters for the Umlego workflow
     * @param outputFolder       the folder where the results should be written
     * @param destinationZoneIds the list of destination zone IDs for which results are being processed
     * @param listeners          the list of listeners to notify about the results
     */
    List<WorkResultHandler<?>> createResultHandler(UmlegoParameters params, String outputFolder, List<String> destinationZoneIds, List<UmlegoListener> listeners);

}
