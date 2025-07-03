package ch.sbb.matsim.umlego.deltat;

/**
 * <p>
 * Calculates the adaptation time ("delta T") between the departure time and a time interval.
 * Either the early or late adaptation time must be zero when called with the same values.
 * Calculated adaptation times should be normalized to the range of -12h to +12h,
 * and the absolute value of the adaptation time must be returned (i.e., an "early adaptation
 * time" must also be positive).
 * </p>
 *
 * <p>
 * Implementations must be thread-safe.
 * </p>
 */
public interface DeltaTCalculator {

	double calculateDeltaTEarly(double departureTime, double intervalStart, double intervalEnd);
	double calculateDeltaTLate(double departureTime, double intervalStart, double intervalEnd);

	default double normalizeDeltaT(double deltaT) {
		// assume wrap-around of 24 hours for schedule
		// normalize the delta in the range of -12h ... +12h
		while (deltaT > 12*3600) {
			deltaT -= 24*3600;
		}
		while (deltaT < -12*3600) {
			deltaT += 24*3600;
		}
		return deltaT;
	}
}
