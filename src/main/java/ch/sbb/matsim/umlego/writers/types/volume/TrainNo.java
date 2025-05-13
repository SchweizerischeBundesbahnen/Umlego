package ch.sbb.matsim.umlego.writers.types.volume;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;

public record TrainNo(
        String operatorCode,
        Id<Departure> departureId) {

}
