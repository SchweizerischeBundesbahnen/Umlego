package ch.sbb.matsim.umlego.workflows.bewerto;

import ch.sbb.matsim.umlego.AbstractWorker;
import ch.sbb.matsim.umlego.FoundRoute;
import ch.sbb.matsim.umlego.RoutingContext;
import ch.sbb.matsim.umlego.UmlegoRouteUtils;
import ch.sbb.matsim.umlego.UmlegoWorkResult;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrixMultiplier;
import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.skims.UmlegoSkimCalculator;
import ch.sbb.matsim.umlego.workflows.bewerto.elasticities.DemandFactorCalculator;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Worker for the Bewerto workflow.
 */
public class BewertoWorker extends AbstractWorker<BewertoWorkItem> {

    /**
     * List of SwissRailRaptor instances, one for each scenario.
     */
    private final List<RoutingContext> scenarios;

    /**
     * Calculator for demand factors.
     */
    private final DemandFactorCalculator factorCalculator;

    public BewertoWorker(BlockingQueue<BewertoWorkItem> workerQueue, UmlegoParameters params, Matrices demand,
        List<RoutingContext> scenarios, List<String> destinationZoneIds, DeltaTCalculator deltaTCalculator,
        DemandFactorCalculator factorCalculator) {
        super(workerQueue, params, destinationZoneIds, demand,
            params.routeSelection().utilityCalculator().createUtilityCalculator(), deltaTCalculator);
        this.scenarios = scenarios;
        this.factorCalculator = factorCalculator;
    }

    @Override
    protected void processOriginZone(BewertoWorkItem item) {

        RoutingContext baseCtx = scenarios.getFirst();

        Map<String, List<FoundRoute>> baseRoutes = process(baseCtx, item.originZone());
        UmlegoWorkResult baseResult = assignDemand(item.originZone(), baseRoutes);

        // Reassign the demand for the filtered interval
        UmlegoWorkResult filteredDemand = assignDemand(item.originZone(), UmlegoRouteUtils.cloneRoutes(baseRoutes),
            params.skims().startTime(), params.skims().endTime(),
            DemandMatrixMultiplier.IDENTITY);

        UmlegoSkimCalculator.INSTANCE.calculateSkims(filteredDemand, baseResult.skims());

        item.baseCase().complete(baseResult);

        for (int i = 1; i < scenarios.size(); i++) {

            RoutingContext ctx = scenarios.get(i);

            Map<String, List<FoundRoute>> foundRoutes = process(ctx, item.originZone());

            UmlegoWorkResult result = assignDemand(item.originZone(), foundRoutes);

            // Reassign the demand for the filtered interval
            UmlegoWorkResult filtered = assignDemand(item.originZone(), UmlegoRouteUtils.cloneRoutes(foundRoutes),
                params.skims().startTime(), params.skims().endTime(),
                DemandMatrixMultiplier.IDENTITY);

            UmlegoSkimCalculator.INSTANCE.calculateSkims(filtered, result.skims());

            item.variants().get(i - 1).complete(result);

            DemandFactorCalculator.Multiplier f = factorCalculator.createMultiplier(baseResult.skims(), result.skims());

            // Induced demand calculation
            UmlegoWorkResult induced = assignDemand(item.originZone(), UmlegoRouteUtils.cloneRoutes(foundRoutes),
                LocalTime.MIN, LocalTime.MAX, f);

            item.induced().get(i - 1).complete(induced);
            item.factors().get(i - 1).complete(f.createResult(item.originZone()));
        }
    }

    private Map<String, List<FoundRoute>> process(RoutingContext ctx, String originZone) {
        Map<String, List<FoundRoute>> foundRoutes = calculateRoutesForZone(ctx, originZone);
        calculateRouteCharacteristics(foundRoutes);
        filterRoutes(foundRoutes);
        calculateOriginality(foundRoutes);
        return foundRoutes;
    }
}
