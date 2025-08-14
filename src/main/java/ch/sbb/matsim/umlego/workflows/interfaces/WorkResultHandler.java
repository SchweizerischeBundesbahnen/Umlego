package ch.sbb.matsim.umlego.workflows.interfaces;

/**
 * Class for handling finished {@link WorkResult}s of {@link WorkItem}s.
 * @param <T> the type of {@link WorkResult} to handle
 */
public interface WorkResultHandler<T extends WorkResult> extends AutoCloseable {

    /**
     * Handles the result of a work item.
     *
     */
    void handleResult(T result);

    /**
     * Closes any resources that have been opened by the handler.
     * Called when all results have been processed.
     */
    @Override
    default void close() throws Exception {
    }

}
