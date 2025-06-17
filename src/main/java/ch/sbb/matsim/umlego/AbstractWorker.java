package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;

import java.util.concurrent.BlockingQueue;

/**
 * Base class for workers that process work items from a blocking queue.
 */
public abstract class AbstractWorker<T extends WorkItem> implements Runnable {

    protected final BlockingQueue<T> workerQueue;

    protected AbstractWorker(BlockingQueue<T> workerQueue) {
        this.workerQueue = workerQueue;
    }

    @Override
    public final void run() {
        while (true) {
            T item = null;
            try {
                item = this.workerQueue.take();
                if (item.originZone() == null) {
                    return;
                }
                processOriginZone(item);
            } catch (InterruptedException | ZoneNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    abstract protected void processOriginZone(T item);

}
