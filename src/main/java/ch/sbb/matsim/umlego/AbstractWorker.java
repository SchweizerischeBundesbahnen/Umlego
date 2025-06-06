package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;

import java.util.concurrent.BlockingQueue;

public abstract class AbstractWorker implements Runnable {

    protected final BlockingQueue<WorkItem> workerQueue;

    protected AbstractWorker(BlockingQueue<WorkItem> workerQueue) {
        this.workerQueue = workerQueue;
    }

    @Override
    public final void run() {
        while (true) {
            WorkItem item = null;
            try {
                item = this.workerQueue.take();
                if (item.originZone() == null) {
                    return;
                }
                WorkResult result = processOriginZone(item);
                item.result().complete(result);
            } catch (InterruptedException | ZoneNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    abstract protected WorkResult processOriginZone(WorkItem item);

}
