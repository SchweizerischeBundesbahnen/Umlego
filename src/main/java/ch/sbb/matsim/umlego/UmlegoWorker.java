package ch.sbb.matsim.umlego;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.umlego.ZoneConnections.ConnectedStop;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
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

    private static final Logger log = LogManager.getLogger(UmlegoWorker.class);

    private final SwissRailRaptor raptor;

    public UmlegoWorker(BlockingQueue<UmlegoWorkItem> workerQueue,
                        UmlegoParameters params,
                        DemandMatrices demand,
                        SwissRailRaptor raptor,
                        RaptorParameters raptorParams,
                        List<String> destinationZoneIds,
                        Map<String, List<ConnectedStop>> stopsPerZone,
                        Map<String, Map<TransitStopFacility, ConnectedStop>> stopLookupPerDestination,
                        DeltaTCalculator deltaTCalculator) {
        super(workerQueue, params, destinationZoneIds, demand, demand.getMatrixNames(), stopsPerZone,
                stopLookupPerDestination, raptorParams,
                params.routeSelection().utilityCalculator().createUtilityCalculator(), deltaTCalculator);

        this.raptor = raptor;
    }

    @Override
    protected void processOriginZone(UmlegoWorkItem workItem) throws ZoneNotFoundException {
        Map<String, List<FoundRoute>> foundRoutes = calculateRoutesForZone(raptor, workItem.originZone());
        calculateRouteCharacteristics(foundRoutes);
        filterRoutes(foundRoutes);
        calculateOriginality(foundRoutes);

        UmlegoWorkResult result = assignDemand(workItem.originZone(), foundRoutes);

        UmlegoSkimCalculator.INSTANCE.calculateSkims(result);

        workItem.result().complete(result);
    }

}
