package ch.sbb.matsim.umlego.demand;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class UnroutableDemandTest {

    @Test
    void testSum() {
        UnroutableDemand unroutableDemand = new UnroutableDemand();
        unroutableDemand.addPart(new UnroutableDemandPart("1", "2", 10));
        unroutableDemand.addPart(new UnroutableDemandPart("1", "3", 30));

        double tolerance = 0.01;
        assertThat(unroutableDemand.sum()).isCloseTo(10.0 + 30.0, within(tolerance));
    }
}