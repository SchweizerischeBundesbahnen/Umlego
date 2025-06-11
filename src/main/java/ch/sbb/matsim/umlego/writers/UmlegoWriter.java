package ch.sbb.matsim.umlego.writers;

import ch.sbb.matsim.umlego.FoundRoute;

/**
 * An interface for a writer for Umlego.
 * <p>
 * The writer is responsible for writing out the routes and their properties to some output
 * format. The output format does not have to be a file, it could also be a database or some
 * other output format.
 * <p>
 * The writer is also responsible for closing any resources it has opened.
 */
public interface UmlegoWriter extends AutoCloseable {

    /**
     * Write out a single route from an origin zone to a destination zone.
     *
     * @param origZone the origin zone
     * @param destZone the destination zone
     * @param route the route
     */
    void writeRoute(String origZone, String destZone, FoundRoute route);


    /**
     * Write the results for an origin-destination pair.
     * No actual data is provided to the function, the writer must retrieve or collect necessary data
     */
    default void writeODPair(String origZone, String destZone) {

    }

    /**
     * Close any resources that have been opened by the writer.
     * This method is called automatically by the {@link AutoCloseable} interface.
     */
    @Override
    void close() throws Exception;

}