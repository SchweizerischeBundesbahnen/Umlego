package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.workflows.interfaces.WorkItem;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Work item for Umlego, which contains one result per origin zone.
 */
public record UmlegoWorkItemEndMarker(
        String originZone
) implements WorkItem {

    @Override
    public List<CompletableFuture<? extends WorkResult>> results() {
        return List.of();
    }
}
