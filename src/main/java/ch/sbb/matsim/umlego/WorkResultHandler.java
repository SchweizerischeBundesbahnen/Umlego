package ch.sbb.matsim.umlego;

/**
 * Class for handling finished {@link WorkResult}s of {@link WorkItem}s.
 * @param <T> the type of {@link WorkResult} to handle
 */
public interface WorkResultHandler<T extends WorkResult> {

    /**
     * Handles the result of a work item.
     *
     */
    void handleResult(T result);

}
