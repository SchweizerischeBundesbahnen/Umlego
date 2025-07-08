package ch.sbb.matsim.umlego;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The {@code WorkItem} interface defines a unit of work that can be processed by a worker.
 */
public interface WorkItem {

    /**
     * Returns the origin zone for this work item.
     */
    String originZone();

    /**
     * Returns a list of CompletableFutures for all results of this work item.
     */
    List<CompletableFuture<? extends WorkResult>> results();

}
