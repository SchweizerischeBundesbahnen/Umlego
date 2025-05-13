package ch.sbb.matsim.umlego.demand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.DemandMatrix;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.matsim.core.utils.collections.Tuple;

class UnroutableDemandStatsTest {

    public static DemandMatrices createFixtureDemandMatrices(Map<Tuple<Integer, Integer>, Double> odDemands) {
        Map<String, Integer> zoneIds = odDemands.keySet().stream()
            .flatMap(tuple -> Stream.of(tuple.getFirst(), tuple.getSecond()))
            .collect(Collectors.toMap(String::valueOf, i -> i, (existing, replacement) -> existing));

        int size = zoneIds.size();
        double[][] data = new double[size][size];
        for (Map.Entry<Tuple<Integer, Integer>, Double> entry : odDemands.entrySet()) {
            int fromIdx = entry.getKey().getFirst();
            int toIdx = entry.getKey().getSecond();
            double value = entry.getValue();
            data[fromIdx][toIdx] = value;
        }

        ZonesLookup lookup = new ZonesLookup(zoneIds);
        DemandMatrix matrix = new DemandMatrix(0, 1, data);
        return new DemandMatrices(List.of(matrix), lookup);
    }


    @Test
    void getOriginTotalDemandMap() throws ZoneNotFoundException {
        UnroutableDemand unroutableDemand = new UnroutableDemand();
        unroutableDemand.addPart(new UnroutableDemandPart("0", "1", 10));
        unroutableDemand.addPart(new UnroutableDemandPart("0", "2", 30));

        Map<Tuple<Integer, Integer>, Double> odDemands = Map.of(
            Tuple.of(0, 5), 10.0,
            Tuple.of(0, 1), 20.0,
            Tuple.of(1, 6), 20.0,
            Tuple.of(2, 7), 30.0,
            Tuple.of(3, 8), 40.0,
            Tuple.of(4, 9), 50.0
        );
        DemandMatrices demandMatrices = createFixtureDemandMatrices(odDemands);

        UnroutableDemandStats udstats = new UnroutableDemandStats(unroutableDemand, demandMatrices);

        assertThat(udstats.getDemandForOrigin()).containsEntry("0", 30.0);
        assertThat(udstats.getDemandForOrigin()).containsEntry("4", 50.0);
    }

    @Test
    void largestUnroutableDemandZone() throws ZoneNotFoundException {
        UnroutableDemand unroutableDemand = new UnroutableDemand();
        unroutableDemand.addPart(new UnroutableDemandPart("0", "2", 10.0));
        unroutableDemand.addPart(new UnroutableDemandPart("1", "3", 750.0));
        unroutableDemand.addPart(new UnroutableDemandPart("1", "4", 30.0));

        Map<Tuple<Integer, Integer>, Double> odDemands = Map.of(
            Tuple.of(0, 2), 10.0,
            Tuple.of(1, 3), 750.0,
            Tuple.of(1, 4), 30.0,
            Tuple.of(1, 0), 40.0
        );
        DemandMatrices demandMatrices = createFixtureDemandMatrices(odDemands);

        UnroutableDemandStats udstats = new UnroutableDemandStats(unroutableDemand, demandMatrices);

        UnroutableDemandZone largestZone = udstats.largestUnroutableDemandZone();
        assertThat(largestZone.fromZone()).isEqualTo("1");
        assertThat(largestZone.demand()).isEqualTo(750.0 + 30.0 + 40.0);
    }

    @Test
    void totalUnroutableDemand() {
        UnroutableDemand unroutableDemand = new UnroutableDemand();
        unroutableDemand.addPart(new UnroutableDemandPart("0", "1", 10));
        unroutableDemand.addPart(new UnroutableDemandPart("0", "2", 30));

        Map<Tuple<Integer, Integer>, Double> odDemands = Map.of(
            Tuple.of(0, 5), 10.0,
            Tuple.of(0, 1), 20.0,
            Tuple.of(1, 6), 20.0,
            Tuple.of(2, 7), 30.0,
            Tuple.of(3, 8), 40.0,
            Tuple.of(4, 9), 50.0
        );
        DemandMatrices demandMatrices = createFixtureDemandMatrices(odDemands);

        UnroutableDemandStats udstats = new UnroutableDemandStats(unroutableDemand, demandMatrices);

        assertThat(udstats.totalUnroutableDemand()).isEqualTo(10.0 + 30.0);
    }

    @Test
    void percentUnroutableDemand() {
        UnroutableDemand unroutableDemand = new UnroutableDemand();
        unroutableDemand.addPart(new UnroutableDemandPart("0", "1", 10.0));
        unroutableDemand.addPart(new UnroutableDemandPart("0", "2", 30.0));

        Map<Tuple<Integer, Integer>, Double> odDemands = Map.of(
            Tuple.of(0, 1), 10.0,
            Tuple.of(0, 2), 30.0,
            Tuple.of(1, 2), 100.0
        );
        DemandMatrices demandMatrices = createFixtureDemandMatrices(odDemands);

        UnroutableDemandStats udstats = new UnroutableDemandStats(unroutableDemand, demandMatrices);

        double tolerance = 0.001;
        assertThat(udstats.percentUnroutableDemand()).isCloseTo((10.0 + 30.0) / (10.0 + 30.0 + 100.0),
            within(tolerance));
    }
}