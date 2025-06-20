package ch.sbb.matsim.umlego;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Work item for Umlego, which contains one result per origin zone.
 */
public record UmlegoWorkItem(
        String originZone,
        CompletableFuture<UmlegoWorkResult> result
) implements WorkItem {

    @Override
    public List<CompletableFuture<? extends WorkResult>> results() {
        return List.of(result);
    }
}
