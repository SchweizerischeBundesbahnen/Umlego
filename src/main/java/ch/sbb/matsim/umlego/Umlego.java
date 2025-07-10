package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.deltat.IntervalBoundaries;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;

/**
 * @author mrieser / Simunto
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Umlego {

    private static final Logger LOG = LogManager.getLogger(Umlego.class);

    private final DemandMatrices demand;
    private final WorkflowFactory workflowFactory;

    /**
     * List of listeners to be notified about found routes.
     */
    private final List<UmlegoListener> listeners = new LinkedList<>();

    private DeltaTCalculator deltaTCalculator = new IntervalBoundaries();

    /**
     * Constructor.
     *
     * @param demand demand matrices
     * @param scenario MATSim object collecting all sort of data
     */
    public Umlego(DemandMatrices demand, Scenario scenario, String zoneConnectionsFile) throws IOException {
        this(demand, new UmlegoWorkflowFactory(demand, scenario, zoneConnectionsFile));
    }

    /**
     * Constructor.
     *
     * @param demand demand matrices
     */
    public Umlego(DemandMatrices demand, WorkflowFactory workflowFactory) {
        this.demand = demand;
        this.workflowFactory = workflowFactory;
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
     * Retrieves the listener of the specified type from the collection of registered listeners.
     *
     * @param <T> the type of the listener to retrieve, which must extend {@code UmlegoListener}
     * @param type the {@code Class} object representing the type of listener to search for
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

    public void run(UmlegoParameters params, int threadCount, String outputFolder) throws ZoneNotFoundException, IOException {
        // TODO ?: Why null values and not just delete the param on the run method?
        var zones = !params.zones().isEmpty() ? params.zones() : null;
        run(zones, zones, params, threadCount, outputFolder);
    }

    public void run(List<String> originZones, List<String> destinationZones, UmlegoParameters params, int threadCount, String outputFolder) throws ZoneNotFoundException, IOException {

        List<String> originZoneIds = originZones == null ? new ArrayList<>(demand.getZones().getAllZoneNos())
            : new ArrayList<>(originZones);
        originZoneIds.sort(String::compareTo);
        List<String> destinationZoneIds =
            destinationZones == null ? new ArrayList<>(demand.getZones().getAllZoneNos())
                : new ArrayList<>(destinationZones);
        destinationZoneIds.sort(String::compareTo);

        // detect relevant stops
        IntSet destinationStopIndices = workflowFactory.computeDestinationStopIndices(destinationZoneIds);
        LOG.info("Detected {} stops as potential destinations", destinationStopIndices.size());

        // prepare queues with work items
		/* Writing might actually be slower than the computation, resulting in more and more
		   memory being used for the found routes until they get written. To prevent
		   OutOfMemoryErrors, we use a blocking queue for the writer with a limited capacity.
		 */
        UmlegoWorkItem workEndMarker = new UmlegoWorkItem(null, null);

        BlockingQueue<WorkItem> workerQueue = new LinkedBlockingQueue<>(5 * threadCount);
        BlockingQueue<WorkItem> writerQueue = new LinkedBlockingQueue<>(4 * threadCount);

        // start worker threads
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threads.length; i++) {
            AbstractWorker worker = workflowFactory.createWorker(
                workerQueue, params, destinationZoneIds, this.deltaTCalculator);

            threads[i] = new Thread(worker);
            threads[i].start();
        }

        // start writer threads
        List<WorkResultHandler<?>> handler = workflowFactory.createResultHandler(params, outputFolder, destinationZoneIds, listeners);
        ResultWorker writerManager = new ResultWorker(writerQueue, handler, originZoneIds);
        new Thread(writerManager).start();

        // submit work items into queues
        for (String originZoneId : originZoneIds) {

            try {
                WorkItem workItem = workflowFactory.createWorkItem(originZoneId);

                if (workItem.results().size() != handler.size()) {
                    throw new IllegalArgumentException(String.format("The number of results in the work item (%d) must match the number of WorkResultHandler (%d).",
                        workItem.results().size(), handler.size()));
                }

                writerQueue.put(workItem);
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
            writerQueue.put(workEndMarker);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            // Wait for the result worker to finish processing all items
            LOG.info("Waiting for result worker to complete...");

            writerManager.waitForCompletion();

            LOG.info("Result pipeline completed.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for result worker to complete", e);
        }
    }

}
