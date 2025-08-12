package ch.sbb.matsim.umlego.workflows.assignment;

import ch.sbb.matsim.umlego.workflows.interfaces.WorkItem;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkResult;
import ch.sbb.matsim.umlego.UmlegoWorkResult;
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
    CompletableFuture<UmlegoWorkResult> result
) implements WorkItem {

    @Override
    public List<CompletableFuture<? extends WorkResult>> results() {

        List<CompletableFuture<? extends WorkResult>> all = new ArrayList<>();
        all.add(result);
        return all;
    }
}
