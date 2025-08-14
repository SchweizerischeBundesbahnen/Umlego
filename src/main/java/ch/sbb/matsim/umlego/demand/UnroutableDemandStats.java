package ch.sbb.matsim.umlego.demand;

import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UnroutableDemandStats {

    private static final double SHARE_LIMIT = 0.95;

    private final UnroutableDemand unroutableDemand;
    private final Matrices matrices;

    private Map<String, Double> originUnroutableDemandMap;

    private static final Logger LOG = LogManager.getLogger(UnroutableDemandStats.class);

    /**
     * Constructor.
     *
     * @param unroutableDemand
     * @param matrices
     */
    public UnroutableDemandStats(UnroutableDemand unroutableDemand, Matrices matrices) {
        this.unroutableDemand = unroutableDemand;
        this.matrices = matrices;
    }

    private Map<String, Double> getUnroutableDemandForOrigin() {
        if (originUnroutableDemandMap == null) {
            originUnroutableDemandMap = unroutableDemand.getParts()
                    .stream()
                    .collect(Collectors.groupingBy(UnroutableDemandPart::fromZone,
                            Collectors.summingDouble(UnroutableDemandPart::demand)));
        }
        return originUnroutableDemandMap;
    }

    Map<String, Double> getDemandForOrigin() throws ZoneNotFoundException {

        Map<String, Double> demand = new HashMap<>();
        for (String zone : this.matrices.getZones().getAllZoneNos()) {
            demand.put(zone, this.matrices.getOriginSum(zone));
        }

        return demand;
    }

    private Map<String, Double> combineAndCalculateShareMap(Map<String, Double> unroutableDemand,
            Map<String, Double> totalDemand) {
        Map<String, Double> result = new HashMap<>();
        for (Map.Entry<String, Double> entry : unroutableDemand.entrySet()) {
            String fromZone = entry.getKey();
            Double unroutable = entry.getValue();
            Double total = totalDemand.getOrDefault(fromZone, 0.0);
            double share;
            if (unroutable != null && total != 0) {
                share = unroutable / total;
            } else {
                share = 0.0;
            }
            result.put(fromZone, share);
        }
        return result;
    }

    /**
     * Origin zone with the highest unroutable demand: name and total Default value for share limit: 0.95
     *
     * @return Map<String, Double> zoneId and total demand or null
     */
    public UnroutableDemandZone largestUnroutableDemandZone() throws ZoneNotFoundException {
        return largestUnroutableDemandZone(SHARE_LIMIT);
    }

    /**
     * Origin zone with the highest unroutable demand: name and total
     *
     * @param limit for share between unroutable demand and total demand
     * @return Map<String, Double> zoneId and total demand or null
     */
    public UnroutableDemandZone largestUnroutableDemandZone(double limit) throws ZoneNotFoundException {
        Map<String, Double> shareMap = combineAndCalculateShareMap(getUnroutableDemandForOrigin(),
            getDemandForOrigin());

        Map<String, Double> filteredShareMap = shareMap.entrySet().stream()
            .filter(entry -> entry.getValue() > limit)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Set<String> keysToKeep = filteredShareMap.keySet();

        Map<String, Double> totalDemandMap = getDemandForOrigin().entrySet().stream()
            .filter(entry -> keysToKeep.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Optional<Entry<String, Double>> maxEntry = totalDemandMap.entrySet()
            .stream()
            .max(Map.Entry.comparingByValue());

        if (maxEntry.isPresent()) {
            double demandForOrigin = maxEntry.get().getValue();
            return new UnroutableDemandZone(maxEntry.get().getKey(),
                demandForOrigin);
        } else {
            LOG.info("No unroutable demand zone found above the limit {}", limit);
            return UnroutableDemandZone.EMPTY;
        }
    }

    /**
     * Total unroutable demand.
     *
     * @return double unroutable demand
     */
    public double totalUnroutableDemand() {
        return unroutableDemand.sum();
    }

    /**
     * Share of unroutable demand
     *
     * @return double percent
     */
    public double percentUnroutableDemand() {
        double totalUnroutableDemand = totalUnroutableDemand();
        double totalDemand = matrices.getSum();
        return totalDemand == 0 ? 0 : totalUnroutableDemand / totalDemand;
    }
}
