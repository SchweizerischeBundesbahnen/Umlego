package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.umlego.UmlegoWorkItem;
import ch.sbb.matsim.umlego.UmlegoWorker;
import ch.sbb.matsim.umlego.ZoneConnections;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

/**
 * Worker for the Bewerto workflow.
 */
public class BewertoWorker extends UmlegoWorker {

    /**
     * List of SwissRailRaptor instances, one for each scenario.
     */
    private final List<SwissRailRaptor> raptor = null;

    public BewertoWorker(BlockingQueue<UmlegoWorkItem> workerQueue, UmlegoParameters params, DemandMatrices demand,
                         SwissRailRaptor raptor, RaptorParameters raptorParams,
                         List<String> destinationZoneIds, Map<String, List<ZoneConnections.ConnectedStop>> stopsPerZone,
                         Map<String, Map<TransitStopFacility, ZoneConnections.ConnectedStop>> stopLookupPerDestination,
                         DeltaTCalculator deltaTCalculator) {
        super(workerQueue, params, demand, raptor, raptorParams, destinationZoneIds, stopsPerZone, stopLookupPerDestination, deltaTCalculator);
    }

}
