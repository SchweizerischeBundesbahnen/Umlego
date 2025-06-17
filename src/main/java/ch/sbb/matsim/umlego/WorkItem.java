package ch.sbb.matsim.umlego;

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
     * Returns a CompletableFuture that will be completed with the last result of processing this work item.
     */
    CompletableFuture<WorkResult> result();

    /**
     * Returns an iterable of CompletableFutures for work items that produce multiple results.
     */
    Iterable<CompletableFuture<WorkResult>> results();


}
