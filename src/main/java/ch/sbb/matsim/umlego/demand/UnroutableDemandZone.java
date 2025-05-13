package ch.sbb.matsim.umlego.demand;

/**
 * Class encapsulating return values.
 *
 * @param fromZone origin zone
 * @param demand total demand (routable + unroutable) for this origin zone.
 */
public record UnroutableDemandZone (
        String fromZone,
        Double demand) {

    public static final UnroutableDemandZone EMPTY = new UnroutableDemandZone(null, null);
}
