package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.umlego.AbstractWorker;
import ch.sbb.matsim.umlego.FoundRoute;
import ch.sbb.matsim.umlego.RoutingContext;
import ch.sbb.matsim.umlego.UmlegoWorkResult;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.skims.UmlegoSkimCalculator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

/**
 * Worker for the Bewerto workflow.
 */
public class BewertoWorker extends AbstractWorker<BewertoWorkItem> {

    /**
     * List of SwissRailRaptor instances, one for each scenario.
     */
    private final List<RoutingContext> scenarios;

    public BewertoWorker(BlockingQueue<BewertoWorkItem> workerQueue, UmlegoParameters params, DemandMatrices demand,
                         List<RoutingContext> scenarios, List<String> destinationZoneIds, DeltaTCalculator deltaTCalculator) {
        super(workerQueue, params, destinationZoneIds, demand, demand.getMatrixNames(),
                params.routeSelection().utilityCalculator().createUtilityCalculator(), deltaTCalculator);
       this.scenarios = scenarios;
    }

    @Override
    protected void processOriginZone(BewertoWorkItem item) {

        // TODO: not yet the full work flow
        Iterator<CompletableFuture<UmlegoWorkResult>> it = item.allResults().iterator();
        for (RoutingContext raptor : scenarios) {

            Map<String, List<FoundRoute>> foundRoutes = calculateRoutesForZone(raptor, item.originZone());
            calculateRouteCharacteristics(foundRoutes);
            filterRoutes(foundRoutes);
            calculateOriginality(foundRoutes);

            UmlegoWorkResult result = assignDemand(item.originZone(), foundRoutes);

            UmlegoSkimCalculator.INSTANCE.calculateSkims(result);

            it.next().complete(result);
        }
    }
}
