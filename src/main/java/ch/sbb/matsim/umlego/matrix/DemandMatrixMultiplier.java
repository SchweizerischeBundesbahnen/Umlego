package ch.sbb.matsim.umlego.matrix;

/**
 * Represents a functional interface used to calculate a demand adjustment factor.
 * This multiplier is applied based on the origin zone, destination zone, and time of the day in minutes.
 */
@FunctionalInterface
public interface DemandMatrixMultiplier {


    /**
     * Calculates an adjustment factor based on the specified zones and time.
     *
     * @param fromZone the origin zone for the calculation
     * @param toZone the destination zone for the calculation
     * @param timeMin the time of day in minutes for which the factor is calculated
     * @return the adjustment factor as a double
     */
    double getFactor(String fromZone, String toZone, int timeMin);

}
