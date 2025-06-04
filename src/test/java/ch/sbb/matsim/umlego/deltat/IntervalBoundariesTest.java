package ch.sbb.matsim.umlego.deltat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class IntervalBoundariesTest {

	private static final double ONEDAY = 86400.0;

	@Test
	public void testIntervalBoundariesCalculations() {
		IntervalBoundaries calculator = new IntervalBoundaries();

		// slightly before interval
		Assertions.assertEquals(100.0, calculator.calculateDeltaTEarly(2900, 3000, 3100));
		Assertions.assertEquals(0.0, calculator.calculateDeltaTLate(2900, 3000, 3100));

		// slightly after interval
		Assertions.assertEquals(0.0, calculator.calculateDeltaTEarly(3200, 3000, 3100));
		Assertions.assertEquals(100.0, calculator.calculateDeltaTLate(3200, 3000, 3100));

		// exactly on interval boundaries
		Assertions.assertEquals(0.0, calculator.calculateDeltaTEarly(3000, 3000, 3100));
		Assertions.assertEquals(0.0, calculator.calculateDeltaTLate(3000, 3000, 3100));

		Assertions.assertEquals(0.0, calculator.calculateDeltaTEarly(3100, 3000, 3100));
		Assertions.assertEquals(0.0, calculator.calculateDeltaTLate(3100, 3000, 3100));

		// exactly in the middle of the interval
		Assertions.assertEquals(0.0, calculator.calculateDeltaTEarly(3050, 3000, 3100));
		Assertions.assertEquals(0.0, calculator.calculateDeltaTLate(3050, 3000, 3100));

		// slightly before interval, but shifted by one or more days
		Assertions.assertEquals(100.0, calculator.calculateDeltaTEarly(2900 + ONEDAY, 3000, 3100));
		Assertions.assertEquals(100.0, calculator.calculateDeltaTEarly(2900 + 2 * ONEDAY, 3000, 3100));
		Assertions.assertEquals(100.0, calculator.calculateDeltaTEarly(2900, 3000 + ONEDAY, 3100 + ONEDAY));
		Assertions.assertEquals(100.0, calculator.calculateDeltaTEarly(2900, 3000 + 2 * ONEDAY, 3100 + 2 * ONEDAY));

		Assertions.assertEquals(0.0, calculator.calculateDeltaTLate(2900 + ONEDAY, 3000, 3100));
		Assertions.assertEquals(0.0, calculator.calculateDeltaTLate(2900 + 2 * ONEDAY, 3000, 3100));
		Assertions.assertEquals(0.0, calculator.calculateDeltaTLate(2900, 3000 + ONEDAY, 3100 + ONEDAY));
		Assertions.assertEquals(0.0, calculator.calculateDeltaTLate(2900, 3000 + 2 * ONEDAY, 3100 + 2 * ONEDAY));

		// slightly after interval, but shifted by one or more days
		Assertions.assertEquals(0.0, calculator.calculateDeltaTEarly(3200 + ONEDAY, 3000, 3100));
		Assertions.assertEquals(0.0, calculator.calculateDeltaTEarly(3200 + 2 * ONEDAY, 3000, 3100));
		Assertions.assertEquals(0.0, calculator.calculateDeltaTEarly(3200, 3000 + ONEDAY, 3100 + ONEDAY));
		Assertions.assertEquals(0.0, calculator.calculateDeltaTEarly(3200, 3000 + 2 * ONEDAY, 3100 + 2 * ONEDAY));

		Assertions.assertEquals(100.0, calculator.calculateDeltaTLate(3200 + ONEDAY, 3000, 3100));
		Assertions.assertEquals(100.0, calculator.calculateDeltaTLate(3200 + 2 * ONEDAY, 3000, 3100));
		Assertions.assertEquals(100.0, calculator.calculateDeltaTLate(3200, 3000 + ONEDAY, 3100 + ONEDAY));
		Assertions.assertEquals(100.0, calculator.calculateDeltaTLate(3200, 3000 + 2 * ONEDAY, 3100 + 2 * ONEDAY));
	}

}