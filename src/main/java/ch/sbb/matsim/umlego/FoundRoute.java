package ch.sbb.matsim.umlego;

public class FoundRoute {
    public final Stop2StopRoute stop2stopRoute;
    public final Connectors.ConnectedStop originConnectedStop;
    public final Connectors.ConnectedStop destinationConnectedStop;
    public final double travelTimeWithAccess;

    public double searchImpedance = Double.NaN; // Suchwiderstand
    public double perceivedJourneyTimeMin = Double.NaN; // Empfundene Reisezeit
    public double demand = 0;
    public double adaptationTime = 0;
    public double originality = 0; // Eigenständigkeit

    public FoundRoute(Stop2StopRoute stop2stopRoute, Connectors.ConnectedStop originConnectedStop, Connectors.ConnectedStop destinationConnectedStop) {
        this.stop2stopRoute = stop2stopRoute;
        this.originConnectedStop = originConnectedStop;
        this.destinationConnectedStop = destinationConnectedStop;
        this.travelTimeWithAccess = stop2stopRoute.travelTimeWithoutAccess + originConnectedStop.walkTime() + destinationConnectedStop.walkTime();
    }

    /**
     * This is a copy constructor. But demand and calculations are not copied so they can be recalculated.
     */
    @SuppressWarnings("CopyConstructorMissesField")
    public FoundRoute(FoundRoute other) {
        this.stop2stopRoute = other.stop2stopRoute;
        this.originConnectedStop = other.originConnectedStop;
        this.destinationConnectedStop = other.destinationConnectedStop;
        this.travelTimeWithAccess = other.travelTimeWithAccess;

        // Copy the calculated values
        this.searchImpedance = other.searchImpedance;
        this.perceivedJourneyTimeMin = other.perceivedJourneyTimeMin;
        this.originality = other.originality;
    }
}
