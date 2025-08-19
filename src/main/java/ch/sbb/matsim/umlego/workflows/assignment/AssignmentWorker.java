package ch.sbb.matsim.umlego.workflows.assignment;

import ch.sbb.matsim.umlego.AbstractWorker;
import ch.sbb.matsim.umlego.FoundRoute;
import ch.sbb.matsim.umlego.RoutingContext;
import ch.sbb.matsim.umlego.UmlegoRouteUtils;
import ch.sbb.matsim.umlego.UmlegoWorkResult;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrixMultiplier;
import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.skims.UmlegoSkimCalculator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Worker for the Bewerto workflow.
 */
public class AssignmentWorker extends AbstractWorker<AssignmentWorkItem> {

    /**
     * List of SwissRailRaptor instances, one for each scenario.
     */
    private final RoutingContext ctx;

    public AssignmentWorker(BlockingQueue<AssignmentWorkItem> workerQueue, UmlegoParameters params, Matrices demand,
        RoutingContext scenario, List<String> destinationZoneIds, DeltaTCalculator deltaTCalculator) {
        super(workerQueue, params, destinationZoneIds, demand,
            params.routeSelection().utilityCalculator().createUtilityCalculator(), deltaTCalculator);
        this.ctx = scenario;
    }

    @Override
    protected void processOriginZone(AssignmentWorkItem workItem) throws ZoneNotFoundException {
        Map<String, List<FoundRoute>> foundRoutes = calculateRoutesForZone(ctx, workItem.originZone());
        calculateRouteCharacteristics(foundRoutes);
        filterRoutes(foundRoutes);
        calculateOriginality(foundRoutes);

        UmlegoWorkResult result = assignDemand(workItem.originZone(), foundRoutes);

        // Reassign the demand for the filtered interval
        UmlegoWorkResult filteredDemand = assignDemand(workItem.originZone(), UmlegoRouteUtils.cloneRoutes(foundRoutes),
            params.skims().startTimeMinute(), params.skims().endTimeMinute(),
            DemandMatrixMultiplier.IDENTITY);

        UmlegoSkimCalculator.INSTANCE.calculateSkims(filteredDemand, result.skims());

        workItem.result().complete(result);
    }

}
