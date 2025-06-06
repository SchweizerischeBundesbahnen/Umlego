package ch.sbb.matsim.umlego;

import java.util.concurrent.CompletableFuture;

public record WorkItem(
        String originZone,
        CompletableFuture<WorkResult> result
) {
}
