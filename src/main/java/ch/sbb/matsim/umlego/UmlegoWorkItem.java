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

    @Override
    public CompletableFuture<WorkResult> result() {
        return results.getLast();
    }

}
