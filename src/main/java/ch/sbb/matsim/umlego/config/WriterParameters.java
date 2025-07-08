package ch.sbb.matsim.umlego.config;

import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.util.Set;

public record WriterParameters(
        double minimalDemandForWriting,
        CompressionType compression,
        Set<UmlegoWriterType> writerTypes
) {
}
