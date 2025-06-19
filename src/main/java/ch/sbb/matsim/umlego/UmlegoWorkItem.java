package ch.sbb.matsim.umlego;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Work item for Umlego, which contains one result per origin zone.
 */
public record UmlegoWorkItem(
        String originZone,
        List<CompletableFuture<WorkResult>> results
) implements WorkItem {

    /**
     * Return the only result of this work item. Results should never contain more than one result.
     */
    public CompletableFuture<WorkResult> result() {
        return results.getLast();
    }

}
