package ch.sbb.matsim.umlego.config;

import ch.sbb.matsim.umlego.workflows.WorkflowEnum;
import java.util.List;

public record UmlegoParameters(
    int maxTransfers,
    int threads,
    SearchImpedanceParameters search,
    PreselectionParameters preselection,
    PerceivedJourneyTimeParameters pjt,
    RouteImpedanceParameters impedance,
    RouteSelectionParameters routeSelection,
    SkimsParameters skims,
    WriterParameters writer,
    List<String> zones,
    WorkflowEnum workflow
) {

}
