package ch.sbb.matsim.umlego.workflows.interfaces;

/**
 * Interface representing a (partial) result of a work item processed by a worker.
 */
public interface WorkResult {

    /**
     * Returns the origin zone for this work item.
     */
    String originZone();

}
