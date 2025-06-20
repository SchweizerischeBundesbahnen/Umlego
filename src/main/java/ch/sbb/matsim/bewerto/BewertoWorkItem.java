package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.umlego.UmlegoWorkResult;
import ch.sbb.matsim.umlego.WorkItem;
import ch.sbb.matsim.umlego.WorkResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A work item of the Bewerto workflow.
 * @param originZone
 */
public record BewertoWorkItem(
        String originZone,
        List<CompletableFuture<UmlegoWorkResult>> allResults
) implements WorkItem {

    @SuppressWarnings("unchecked")
    @Override
    public List<CompletableFuture<? extends WorkResult>> results() {
        return (List<CompletableFuture<? extends WorkResult>>) (Object) allResults;
    }
}
