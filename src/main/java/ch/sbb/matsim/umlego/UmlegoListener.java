package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

/**
 * Interface representing a listener for processing found routes.
 */
public interface UmlegoListener {

    void processRoute(String origZone, String destZone, FoundRoute route);

    /**
     * Called when the processing of routes is finished.
     */
    default void finish() {
        // Default implementation does nothing
    }

}