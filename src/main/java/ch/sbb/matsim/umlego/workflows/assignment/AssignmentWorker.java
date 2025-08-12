package ch.sbb.matsim.umlego.workflows.assignment;

import ch.sbb.matsim.umlego.AbstractWorker;
import ch.sbb.matsim.umlego.FoundRoute;
import ch.sbb.matsim.umlego.RoutingContext;
import ch.sbb.matsim.umlego.UmlegoRouteUtils;
import ch.sbb.matsim.umlego.workflows.umlego.UmlegoWorkResult;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.DemandMatrixMultiplier;
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
    private final RoutingContext scenario;

    public AssignmentWorker(BlockingQueue<AssignmentWorkItem> workerQueue, UmlegoParameters params, DemandMatrices demand,
        RoutingContext scenario, List<String> destinationZoneIds, DeltaTCalculator deltaTCalculator) {
        super(workerQueue, params, destinationZoneIds, demand, demand.getMatrixNames(),
            params.routeSelection().utilityCalculator().createUtilityCalculator(), deltaTCalculator);
        this.scenario = scenario;
    }

    @Override
    protected void processOriginZone(AssignmentWorkItem item) {

        RoutingContext baseCtx = scenario;

        Map<String, List<FoundRoute>> baseRoutes = process(baseCtx, item.originZone());
        UmlegoWorkResult baseResult = assignDemand(item.originZone(), baseRoutes);

        // Reassign the demand for the filtered interval
        UmlegoWorkResult filteredDemand = assignDemand(item.originZone(), UmlegoRouteUtils.cloneRoutes(baseRoutes),
            params.skims().startTime(), params.skims().endTime(),
            DemandMatrixMultiplier.IDENTITY);

        UmlegoSkimCalculator.INSTANCE.calculateSkims(filteredDemand, baseResult.skims());

        item.baseCase().complete(baseResult);

    }

    private Map<String, List<FoundRoute>> process(RoutingContext ctx, String originZone) {
        Map<String, List<FoundRoute>> foundRoutes = calculateRoutesForZone(ctx, originZone);
        calculateRouteCharacteristics(foundRoutes);
        filterRoutes(foundRoutes);
        calculateOriginality(foundRoutes);
        return foundRoutes;
    }
}
