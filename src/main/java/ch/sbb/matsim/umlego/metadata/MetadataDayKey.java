package ch.sbb.matsim.umlego.metadata;

public enum MetadataDayKey {
    // Operational metadata
    TIMESTAMP,
    RUNTIME,
    STATUS,
    // Calender infos
    BASE_DEMAND,
    CORRECTIONS,
    WEEKDAY,
    // Global stats.
    TRANSFERS_WEIGHTED,
    ADAPTATION_TIME_WEIGHTED,
    ROUTED_DEMAND,
    PERSON_KM,
    PERSON_HOURS,
    UNROUTABLE_DEMAND,
    SHARE_UNROUTABLE_DEMAND,
    LARGEST_UNROUTABLE_ZONE,
    DEMAND_LARGEST_UNROUTABLE_ZONE,
    // Validation criteria
    VALID_AUTO,
    VALID_MANUAL
}
