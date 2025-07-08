package ch.sbb.matsim.umlego.config;

import java.util.Set;

public record WriterParameters(
    double minimalDemandForWriting,
    CompressionType compression,
    Set<UmlegoWriterType> writerTypes
) {

}
