package ch.sbb.matsim.umlego;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.umlego.ZoneConnections.ConnectedStop;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.DemandMatrixMultiplier;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.skims.UmlegoSkimCalculator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Umlego workflow with routing and demand assignment for a single origin zone.
 */
public class UmlegoWorker extends AbstractWorker<UmlegoWorkItem> {

    private final RoutingContext ctx;

    public UmlegoWorker(BlockingQueue<UmlegoWorkItem> workerQueue,
                        UmlegoParameters params,
                        DemandMatrices demand,
                        RoutingContext ctx,
                        List<String> destinationZoneIds,
                        DeltaTCalculator deltaTCalculator) {
        super(workerQueue, params, destinationZoneIds, demand, demand.getMatrixNames(),
                params.routeSelection().utilityCalculator().createUtilityCalculator(), deltaTCalculator);
        this.ctx = ctx;
    }

    @Override
    protected void processOriginZone(UmlegoWorkItem workItem) throws ZoneNotFoundException {
        Map<String, List<FoundRoute>> foundRoutes = calculateRoutesForZone(ctx, workItem.originZone());
        calculateRouteCharacteristics(foundRoutes);
        filterRoutes(foundRoutes);
        calculateOriginality(foundRoutes);

        UmlegoWorkResult result = assignDemand(workItem.originZone(), foundRoutes);

        // Reassign the demand for the filtered interval
        UmlegoWorkResult filteredDemand = assignDemand(workItem.originZone(), UmlegoRouteUtils.cloneRoutes(foundRoutes),
                params.skims().startTime(), params.skims().endTime(),
                DemandMatrixMultiplier.IDENTITY);

        UmlegoSkimCalculator.INSTANCE.calculateSkims(filteredDemand, result.skims());

        workItem.result().complete(result);
    }

}
