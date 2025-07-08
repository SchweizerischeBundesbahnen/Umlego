package ch.sbb.matsim.umlego.matrix;

/**
 * Exception thrown when a specified zone cannot be found in {@link ZonesLookup}.
 * <p>
 * This exception is unchecked because the error is typically not recoverable.
 */
public class ZoneNotFoundException extends RuntimeException {

    public ZoneNotFoundException(String zone) {
        super("Zone '" + zone + "' not found.");
    }
}
