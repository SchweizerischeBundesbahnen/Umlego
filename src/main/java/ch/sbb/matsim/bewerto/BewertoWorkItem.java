package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.umlego.UmlegoWorkResult;
import ch.sbb.matsim.umlego.WorkItem;
import ch.sbb.matsim.umlego.WorkResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A work item of the Bewerto workflow.
 * @param originZone
 */
public record BewertoWorkItem(
        String originZone,
        CompletableFuture<UmlegoWorkResult> baseCase,
        List<CompletableFuture<UmlegoWorkResult>> variants,
        List<CompletableFuture<UmlegoWorkResult>> induced,
        List<CompletableFuture<BewertoWorkResult>> factors
) implements WorkItem {

    @Override
    public List<CompletableFuture<? extends WorkResult>> results() {

        List<CompletableFuture<? extends WorkResult>> all = new ArrayList<>();
        all.add(baseCase);
        for (int i = 0; i < variants.size(); i++) {
            all.add(variants.get(i));
            all.add(induced.get(i));
            all.add(factors.get(i));
        }

        return all;
    }
}
