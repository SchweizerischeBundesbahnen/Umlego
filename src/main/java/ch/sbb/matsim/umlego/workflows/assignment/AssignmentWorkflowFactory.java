package ch.sbb.matsim.umlego.workflows.assignment;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import ch.sbb.matsim.umlego.AbstractWorker;
import ch.sbb.matsim.umlego.Connectors;
import ch.sbb.matsim.umlego.Connectors.ConnectedStop;
import ch.sbb.matsim.umlego.RoutingContext;
import ch.sbb.matsim.umlego.UmlegoListener;
import ch.sbb.matsim.umlego.UmlegoUtils;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkResultHandler;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkflowFactory;
import ch.sbb.matsim.umlego.writers.ResultWriter;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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

    private final Matrices demand;
    private final Scenario scenario;
    private final RaptorParameters raptorParams;
    private final SwissRailRaptorData raptorData;
    private final Map<String, List<Connectors.ConnectedStop>> stopsPerZone;
    private Map<String, Map<TransitStopFacility, ConnectedStop>> stopLookupPerDestination;

    public AssignmentWorkflowFactory(Matrices demand, Map<String, List<Connectors.ConnectedStop>> stopsPerZone, Scenario baseCase) {
        this.demand = demand;
        this.raptorParams = UmlegoUtils.getRaptorParameters(baseCase);
        this.raptorData = UmlegoUtils.getRaptorData(baseCase);
        this.stopsPerZone = stopsPerZone;
        this.stopLookupPerDestination = new HashMap<>();
        this.scenario = baseCase;

    }

    public AssignmentWorkflowFactory(Matrices demand, String zoneConnectionsFile, Scenario baseCase) throws IOException {
        this(demand, UmlegoUtils.readConnectors(zoneConnectionsFile, baseCase.getTransitSchedule()), baseCase);
    }

    @Override
    public AbstractWorker<AssignmentWorkItem> createWorker(BlockingQueue<AssignmentWorkItem> workerQueue, UmlegoParameters params, List<String> destinationZoneIds, DeltaTCalculator deltaTCalculator) {
        SwissRailRaptor raptor = new SwissRailRaptor.Builder(raptorData, this.scenario.getConfig()).build();
        RoutingContext ctx = new RoutingContext(raptor, raptorParams, stopsPerZone, stopLookupPerDestination);

        return new AssignmentWorker(workerQueue, params, demand, ctx, destinationZoneIds, deltaTCalculator);

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

    @Override
    public IntSet computeDestinationStopIndices(List<String> destinationZoneIds) {

        IntSet destinationStopIndices = new IntOpenHashSet();
        for (String zoneId : destinationZoneIds) {
            List<TransitStopFacility> stops = this.stopsPerZone.getOrDefault(zoneId, List.of()).stream()
                .map(Connectors.ConnectedStop::stopFacility).toList();
            for (TransitStopFacility stop : stops) {
                destinationStopIndices.add(stop.getId().index());
            }
        }

        this.stopLookupPerDestination = UmlegoUtils.getStopLookupPerDestination(destinationZoneIds, this.stopsPerZone);

        return destinationStopIndices;
    }
}
