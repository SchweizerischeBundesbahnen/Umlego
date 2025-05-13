package ch.sbb.matsim.umlego.config;

/**
 * Custom exception to indicate that the required saison ID is not on snowflake
 */
public class SaisonRunIDNotOnSnowflakeException extends RuntimeException {
    public SaisonRunIDNotOnSnowflakeException(String message) {
        super(message);
    }
}

