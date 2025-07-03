package ch.sbb.matsim.umlego.deltat;

public class IntervalCenter implements DeltaTCalculator {

	@Override
	public double calculateDeltaTEarly(double departureTime, double intervalStart, double intervalEnd) {
		double center = (intervalStart + intervalEnd) / 2;
		double delta = departureTime - center;
		delta = this.normalizeDeltaT(delta);

		if (delta < 0) {
			return Math.abs(delta);
		}
		return 0.0;
	}

	@Override
	public double calculateDeltaTLate(double departureTime, double intervalStart, double intervalEnd) {
		double center = (intervalStart + intervalEnd) / 2;
		double delta = departureTime - center;
		delta = this.normalizeDeltaT(delta);

		if (delta > 0) {
			return delta;
		}
		return 0.0;
	}
}
