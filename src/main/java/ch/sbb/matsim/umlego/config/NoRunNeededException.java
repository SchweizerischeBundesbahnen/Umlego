package ch.sbb.matsim.umlego.config;

/**
 * Custom exception to indicate that no run is needed.
 */
public class NoRunNeededException extends RuntimeException {
    public NoRunNeededException(String message) {
        super(message);
    }
}

