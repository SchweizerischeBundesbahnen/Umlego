package ch.sbb.matsim.umlego.writers.types.volume;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class JourneyItem {

    private final Id<TransitStopFacility> fromStopFacilityId;
    private final Id<TransitStopFacility> toStopFacilityId;
    private final String fromStopName;
    private final String toStopName;
    private final double arrival;
    private final double departure;
    private final int index;
    private double volume;
    private double boarding;
    private double alighting;
    private double originBoarding;
    private double destinationAlighting;

    public JourneyItem(Id<TransitStopFacility> fromStopFacilityId, Id<TransitStopFacility> toStopFacilityId,
        String fromStopName, String toStopName, double arrival, double departure, int index) {
        this.fromStopFacilityId = fromStopFacilityId;
        this.toStopFacilityId = toStopFacilityId;
        this.fromStopName = fromStopName;
        this.toStopName = toStopName;
        this.arrival = arrival;
        this.departure = departure;
        this.index = index;
        this.volume = 0;
        this.boarding = 0;
        this.alighting = 0;
        this.originBoarding = 0;
        this.destinationAlighting = 0;
    }

    public JourneyItem(TransitRouteStop currentStop, TransitRouteStop nextStop, Departure departure, int index) {
        this(
            currentStop.getStopFacility().getId(),
            nextStop == null ? null : nextStop.getStopFacility().getId(),
            currentStop.getStopFacility().getName(),
            nextStop == null ? null : nextStop.getStopFacility().getName(),
            currentStop.getArrivalOffset().orElse(0f) + departure.getDepartureTime(),
            currentStop.getDepartureOffset().orElse(0f) + departure.getDepartureTime(),
            index
        );
    }

    public void addDemand(double demand) {
        this.volume += demand;
    }

    public void addBoarding(double demand) {
        this.boarding += demand;
    }

    public void addAlighting(double demand) {
        this.alighting += demand;
    }

    public void addOriginBoarding(double demand) {
        this.originBoarding += demand;
    }

    public void addDestinationAlighting(double demand) {
        this.destinationAlighting += demand;
    }

    public Id<TransitStopFacility> getFromStopFacilityId() {
        return fromStopFacilityId;
    }

    public Id<TransitStopFacility> getToStopFacilityId() {
        return toStopFacilityId;
    }

    public String getFromStopName() {
        return fromStopName;
    }

    public String getToStopName() {
        return toStopName;
    }

    public double getArrival() {
        return arrival;
    }

    public double getDeparture() {
        return departure;
    }

    public int getIndex() {
        return index;
    }

    public double getVolume() {
        return volume;
    }

    public double getBoarding() {
        return boarding;
    }

    public double getAlighting() {
        return alighting;
    }

    public double getOriginBoarding() {
        return originBoarding;
    }

    public double getDestinationAlighting() {
        return destinationAlighting;
    }

}