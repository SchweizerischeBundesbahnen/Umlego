package ch.sbb.matsim.umlego.writers;

import ch.sbb.matsim.umlego.demand.UnroutableDemandStats;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;

/**
 * Interface for writing unroutable demand metadata.
 * Implementations can write to different destinations (e.g., database, JSON file).
 */
public interface UnroutableDemandStatsWriter {
    void write(UnroutableDemandStats stats) throws ZoneNotFoundException;
} 