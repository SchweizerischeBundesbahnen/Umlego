package ch.sbb.matsim.umlego.metadata;

import java.time.LocalDate;

public record MetadataDay(
    String runId,
    LocalDate targetDate,
    String key,
    String value) {

}

