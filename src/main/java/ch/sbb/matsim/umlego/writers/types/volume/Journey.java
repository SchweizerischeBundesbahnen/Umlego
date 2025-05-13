package ch.sbb.matsim.umlego.writers.types.volume;

import java.util.ArrayList;

public record Journey(
        TrainNo trainNo,
        ArrayList<JourneyItem> items) {

}
