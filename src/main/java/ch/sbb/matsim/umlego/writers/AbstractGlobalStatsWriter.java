package ch.sbb.matsim.umlego.writers;

import static ch.sbb.matsim.umlego.Hafas2UmlegoSchedule.TSYS_STATS_GROUP;

import ch.sbb.matsim.routing.pt.raptor.RaptorRoute.RoutePart;
import ch.sbb.matsim.umlego.Umlego.FoundRoute;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Abstract base class for global statistics writers. Responsible for accumulating route statistics to generate global
 * metrics such as person-kilometers and person-hours.
 */
public abstract class AbstractGlobalStatsWriter implements UmlegoWriterInterface {

    private static final Logger LOG = LogManager.getLogger(AbstractGlobalStatsWriter.class);

    private static final String PERSON_KM_KEY = "PersonKm";
    private static final String PERSON_HOURS_KEY = "PersonHours";

    public Map<String, StatisticsAccumulator> statsMap = new HashMap<>();
    public double totalWeightedAdaptationTime = 0.0;
    public double totalWeightedTransfers = 0.0;
    public double totalDemand = 0.0;

    /**
     * Collects and accumulates statistics for each route to calculate global statistics.
     *
     * @param origZone The origin zone of the route.
     * @param destZone The destination zone of the route.
     * @param route The FoundRoute object containing details of the route for which statistics are being
     *         calculated.
     */
    @Override
    public void writeRoute(String origZone, String destZone, FoundRoute route) {
        try {
            double demand = route.demand.getDouble(destZone);

            totalWeightedAdaptationTime += demand * route.adaptationTime.getDouble(destZone);
            totalWeightedTransfers += demand * route.transfers;
            totalDemand += demand;

            for (RoutePart part : route.routeParts) {
                TransitRoute transitRoute = part.route;
                if (transitRoute != null) {
                    String tSysGroup = (String) transitRoute.getAttributes().getAttribute(TSYS_STATS_GROUP);
                    StatisticsAccumulator accumulator = statsMap.computeIfAbsent(tSysGroup,
                            t -> new StatisticsAccumulator());

                    double euclideanDistance = calculateRoutePartEuclideanDistance(part);
                    accumulator.totalPersonKm += demand * euclideanDistance;

                    double travelTimeHours = (part.arrivalTime - part.vehicleDepTime) / 3600.0;
                    accumulator.totalPersonHours += demand * travelTimeHours;
                }
            }
        } catch (Exception e) {
            String errorMessage = String.format(
                    "Error while writing route statistics for originZone: %s, destinationZone: %s", origZone, destZone);
            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * Calculates the Euclidean distance between the origin and destination of a route part.
     *
     * @param part The {@link RoutePart} containing details of the route part.
     * @return The Euclidean distance between the origin and destination.
     */
    protected double calculateRoutePartEuclideanDistance(RoutePart part) {
        TransitStopFacility origin = part.fromStop;
        TransitStopFacility destination = part.toStop;
        double dx = origin.getCoord().getX() - destination.getCoord().getX();
        double dy = origin.getCoord().getY() - destination.getCoord().getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Holds accumulated statistics such as total person-kilometers and total person-hours.
     */
    public static class StatisticsAccumulator {

        public double totalPersonKm = 0.0;
        public double totalPersonHours = 0.0;

        /**
         * Default constructor for {@link StatisticsAccumulator}.
         */
        public StatisticsAccumulator() {
            LOG.debug("Initialized new StatisticsAccumulator.");
        }

        /**
         * Calculates the global statistics based on accumulated values.
         *
         * @return A map containing the global statistics.
         */
        Map<String, Double> calculateGlobalStats() {
            Map<String, Double> stats = new HashMap<>();
            stats.put(PERSON_KM_KEY, totalPersonKm);
            stats.put(PERSON_HOURS_KEY, totalPersonHours);
            LOG.debug("Calculated global stats: {}", stats);
            return stats;
        }
    }
}
