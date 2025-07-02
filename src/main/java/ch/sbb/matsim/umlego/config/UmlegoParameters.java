package ch.sbb.matsim.umlego.config;

public record UmlegoParameters(
        int maxTransfers,
        int threads,
        SearchImpedanceParameters search,
        PreselectionParameters preselection,
        PerceivedJourneyTimeParameters pjt,
        RouteImpedanceParameters impedance,
        RouteSelectionParameters routeSelection,
        SkimsParameters skims,
        WriterParameters writer
) {

}
