package ch.sbb.matsim.umlego.writers.types.skim;

/**
 * Origin-Destination zone pair used i.e. in GROUPING operations.
 */
public record ODPair(
    String fromZone,
    String toZone) {

}
