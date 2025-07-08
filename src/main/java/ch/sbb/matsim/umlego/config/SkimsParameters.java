package ch.sbb.matsim.umlego.config;

import java.time.LocalTime;

/**
 * Parameters for the computation of skim matrices.
 */
public record SkimsParameters(
        LocalTime startTime,
        LocalTime endTime
) {

}
