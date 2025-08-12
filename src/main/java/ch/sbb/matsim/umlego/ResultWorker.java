package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.workflows.interfaces.WorkItem;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkResult;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkResultHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * The {@code UmlegoResultWorker} class is responsible for processing work results and passing them to {@link WorkResultHandler}.
 */
public class ResultWorker implements Runnable {

    private static final Logger LOG = LogManager.getLogger(ResultWorker.class);

    private final BlockingQueue<WorkItem> queue;
    private final List<WorkResultHandler<?>> handlers;
    private final List<String> originZoneIds;
    private final CountDownLatch completionLatch = new CountDownLatch(1);

    public ResultWorker(BlockingQueue<WorkItem> queue, List<WorkResultHandler<?>> handlers, List<String> originZoneIds) {
        this.queue = queue;
        this.handlers = handlers;
        this.originZoneIds = originZoneIds;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {

        int totalItems = this.originZoneIds.size();
        int counter = 0;

        while (true) {

            try {
                WorkItem item = this.queue.take();

                if (item.originZone() == null) {
                    // end marker, the work is done
                    break;
                }

                Iterator<WorkResultHandler<?>> it = handlers.iterator();
                for (CompletableFuture<? extends WorkResult> result : item.results()) {

                    WorkResult wr = result.get();
                    WorkResultHandler<? super WorkResult> handler = (WorkResultHandler<? super WorkResult>) it.next();
                    handler.handleResult(wr);
                }

                LOG.info(" - finished processing zone {} ({}/{})", item.originZone(), ++counter, totalItems);

            } catch (InterruptedException e) {
                LOG.error("Worker interrupted while waiting for work item", e);
                Thread.currentThread().interrupt(); // restore the interrupted status
                break;
            } catch (Exception e) {
                LOG.error("Error processing work item", e);
            }
        }

        // Close all handlers
        for (WorkResultHandler<?> handler : handlers) {
            try {
                handler.close();
            } catch (Exception e) {
                LOG.error("Error closing handler", e);
            }
        }

        // Signal that the worker has completed its task
        completionLatch.countDown();
    }

    /**
     * Waits for this worker to finish processing all items.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void waitForCompletion() throws InterruptedException {
        completionLatch.await();
    }

}
