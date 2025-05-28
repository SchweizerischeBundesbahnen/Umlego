package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

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
     * Called when the processing of routes is finished.
     */
    default void finish() {
        // Default implementation does nothing
    }

}