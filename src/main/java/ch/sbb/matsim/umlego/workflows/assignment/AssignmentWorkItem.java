package ch.sbb.matsim.umlego.workflows.assignment;

import ch.sbb.matsim.umlego.workflows.umlego.UmlegoWorkResult;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkItem;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A work item of the Bewerto workflow.
 *
 * @param originZone
 */
public record AssignmentWorkItem(
    String originZone,
    CompletableFuture<UmlegoWorkResult> baseCase
) implements WorkItem {

    @Override
    public List<CompletableFuture<? extends WorkResult>> results() {

        List<CompletableFuture<? extends WorkResult>> all = new ArrayList<>();
        all.add(baseCase);
        return all;
    }
}
