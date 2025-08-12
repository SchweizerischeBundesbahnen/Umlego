package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.workflows.interfaces.WorkResult;

/**
 * Interface representing a listener for processing found routes.
 */
public interface UmlegoListener {

    /**
     * Processes a found route from an origin zone to a destination zone.
     *
     * @param origZone the origin zone
     * @param destZone the destination zone
     * @param route    the found route
     */
    void processRoute(String origZone, String destZone, FoundRoute route);

    /**
     * Processes an origin-destination pair without any specific route data.
     * This method is called when all routes for a specific origin-destination pair have been processed.
     *
     * @param origZone the origin zone
     * @param destZone the destination zone
     */
    default void processODPair(String origZone, String destZone) {
    }

    /**
     * Generic method to process the result of a work item for a specific destination zone.
     */
    default void processResult(WorkResult result, String destZone) {
        // Default implementation does nothing.
        // Subclasses can override this method to provide specific behavior.
    }

    /**
     * Called when the processing of routes is finished.
     */
    default void finish() throws Exception {
        // Default implementation does nothing
    }

}