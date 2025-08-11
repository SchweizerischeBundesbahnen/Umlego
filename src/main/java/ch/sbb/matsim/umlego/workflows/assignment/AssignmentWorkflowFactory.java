package ch.sbb.matsim.umlego.workflows.assignment;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import ch.sbb.matsim.umlego.AbstractWorker;
import ch.sbb.matsim.umlego.Connectors;
import ch.sbb.matsim.umlego.RoutingContext;
import ch.sbb.matsim.umlego.UmlegoListener;
import ch.sbb.matsim.umlego.UmlegoUtils;
import ch.sbb.matsim.umlego.WorkResultHandler;
import ch.sbb.matsim.umlego.WorkflowFactory;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.writers.ResultWriter;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Factory for the bewerto workflow.
 * <p>
 * The workflow processed multiple scenarios at once, computing the routes and assigning the demand once for all scenarios.
 */
public class AssignmentWorkflowFactory implements WorkflowFactory<AssignmentWorkItem> {

    private final DemandMatrices demand;
    private final String zoneConnectionsFile;

    private final RaptorParameters raptorParams;
    private final Scenario scenario;

    /**
     * Save prepared routing contexts for each scenario.
     */
    private RoutingContext ctx;
    private SwissRailRaptorData raptorData;

    public AssignmentWorkflowFactory(DemandMatrices demand, String zoneConnectionsFile, Scenario baseCase) {
        this.demand = demand;
        this.zoneConnectionsFile = zoneConnectionsFile;
        this.scenario = baseCase;

        // prepare SwissRailRaptor
        // TODO: these parameters could be added to a central location.
        raptorParams = RaptorUtils.createParameters(this.scenario.getConfig());
        raptorParams.setTransferPenaltyFixCostPerTransfer(0.01);
        raptorParams.setTransferPenaltyMinimum(0.01);
        raptorParams.setTransferPenaltyMaximum(0.01);

        RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(scenario.getConfig());
        raptorConfig.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
        // make sure SwissRailRaptor does not add any more transfers than what is specified in minimal transfer times:
        raptorConfig.setBeelineWalkConnectionDistance(10.0);

        this.raptorData = SwissRailRaptorData.create(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
            raptorConfig, scenario.getNetwork(), null);

    }

    @Override
    public IntSet computeDestinationStopIndices(List<String> destinationZoneIds) throws IOException {

        Map<String, List<Connectors.ConnectedStop>> stopsPerZone = UmlegoUtils.readConnectors(zoneConnectionsFile, scenario.getTransitSchedule());
        Map<String, Map<TransitStopFacility, Connectors.ConnectedStop>> stopLookupPerDestination = new LinkedHashMap<>();

        // Build the destination stop lookup map
        for (String destinationZoneId : destinationZoneIds) {
            List<Connectors.ConnectedStop> stopsPerDestinationZone = stopsPerZone.getOrDefault(destinationZoneId, List.of());
            Map<TransitStopFacility, Connectors.ConnectedStop> destinationStopLookup = new HashMap<>();
            for (Connectors.ConnectedStop stop : stopsPerDestinationZone) {
                destinationStopLookup.put(stop.stopFacility(), stop);
            }
            stopLookupPerDestination.put(destinationZoneId, destinationStopLookup);
        }

        ctx = new RoutingContext(null, null, stopsPerZone, stopLookupPerDestination);

        // Use stop indices from the first scenario
        IntSet destinationStopIndices = new IntOpenHashSet();
        for (String zoneId : destinationZoneIds) {
            List<TransitStopFacility> stops = ctx.stopsPerZone().getOrDefault(zoneId, List.of()).stream()
                .map(Connectors.ConnectedStop::stopFacility).toList();
            for (TransitStopFacility stop : stops) {
                destinationStopIndices.add(stop.getId().index());
            }
        }

        return destinationStopIndices;
    }

    @Override
    public AbstractWorker<AssignmentWorkItem> createWorker(BlockingQueue<AssignmentWorkItem> workerQueue, UmlegoParameters params, List<String> destinationZoneIds, DeltaTCalculator deltaTCalculator) {

        RoutingContext routingContext = new RoutingContext(
            new SwissRailRaptor.Builder(raptorData, this.scenario.getConfig()).build(),
            raptorParams,
            ctx.stopsPerZone(),
            ctx.stopLookupPerDestination()
        );

        return new AssignmentWorker(
            workerQueue, params, demand, routingContext, destinationZoneIds, deltaTCalculator
        );
    }

    @Override
    public AssignmentWorkItem createWorkItem(String originZone) {

        return new AssignmentWorkItem(originZone,
            new CompletableFuture<>());
    }

    @Override
    public List<? extends WorkResultHandler<?>> createResultHandler(UmlegoParameters params, String outputFolder, List<String> destinationZoneIds, List<UmlegoListener> listeners) {

        List<WorkResultHandler<?>> handler = new ArrayList<>();

        // Base case results
        handler.add(new ResultWriter(
            outputFolder + "/" + scenario.getConfig().controller().getRunId(),
            scenario.getTransitSchedule(),
            listeners,
            params.writer(),
            destinationZoneIds
        ));

        return handler;
    }
}
