package ch.sbb.matsim.umlego.workflows.bewerto;

import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import ch.sbb.matsim.umlego.AbstractWorker;
import ch.sbb.matsim.umlego.Connectors;
import ch.sbb.matsim.umlego.Connectors.ConnectedStop;
import ch.sbb.matsim.umlego.RoutingContext;
import ch.sbb.matsim.umlego.UmlegoListener;
import ch.sbb.matsim.umlego.UmlegoUtils;
import ch.sbb.matsim.umlego.UmlegoWorkResult;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.workflows.bewerto.config.BewertoParameters;
import ch.sbb.matsim.umlego.workflows.bewerto.elasticities.DemandFactorCalculator;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkResultHandler;
import ch.sbb.matsim.umlego.workflows.interfaces.WorkflowFactory;
import ch.sbb.matsim.umlego.writers.ResultWriter;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Factory for the bewerto workflow.
 * <p>
 * The workflow processed multiple scenarios at once, computing the routes and assigning the demand once for all scenarios.
 */
public class BewertoWorkflowFactory implements WorkflowFactory<BewertoWorkItem> {

    private final Matrices demand;
    private final DemandFactorCalculator demandFactorCalculator;
    private final String zoneConnectionsFile;

    private final RaptorParameters raptorParams;
    private final List<Scenario> scenarios;

    /**
     * Save prepared routing contexts for each scenario.
     */
    private final List<RoutingContext> ctxs = new ArrayList<>();
    private final List<SwissRailRaptorData> raptorData = new ArrayList<>();

    public BewertoWorkflowFactory(BewertoParameters parameters, Matrices demand, String zoneConnectionsFile,
        Scenario baseCase, List<Scenario> variants) throws IOException {

        this.demand = demand;
        this.zoneConnectionsFile = zoneConnectionsFile;
        this.demandFactorCalculator = new DemandFactorCalculator(parameters.getElasticities(), demand);
        this.scenarios = new ArrayList<>(variants);
        this.scenarios.addFirst(baseCase);

        // prepare SwissRailRaptor
        raptorParams = UmlegoUtils.getRaptorParameters(scenarios.getFirst());

        for (Scenario scenario : scenarios) {
            SwissRailRaptorData d = UmlegoUtils.getRaptorData(scenario);
            raptorData.add(d);
        }

    }

    @Override
    public IntSet computeDestinationStopIndices(List<String> destinationZoneIds) throws IOException {
        for (Scenario scenario : scenarios) {
            Map<String, List<ConnectedStop>> stopsPerZone = UmlegoUtils.readConnectors(zoneConnectionsFile, scenario.getTransitSchedule());
            Map<String, Map<TransitStopFacility, Connectors.ConnectedStop>> stopLookupPerDestination = UmlegoUtils.getStopLookupPerDestination(destinationZoneIds, stopsPerZone);
            ctxs.add(new RoutingContext(null, null, stopsPerZone, stopLookupPerDestination));
        }

        // Use stop indices from the first scenario
        IntSet destinationStopIndices = new IntOpenHashSet();
        for (String zoneId : destinationZoneIds) {
            List<TransitStopFacility> stops = ctxs.getFirst().stopsPerZone().getOrDefault(zoneId, List.of()).stream()
                .map(Connectors.ConnectedStop::stopFacility).toList();
            for (TransitStopFacility stop : stops) {
                destinationStopIndices.add(stop.getId().index());
            }
        }

        return destinationStopIndices;

    }

    @Override
    public AbstractWorker<BewertoWorkItem> createWorker(BlockingQueue<BewertoWorkItem> workerQueue, UmlegoParameters params, List<String> destinationZoneIds, DeltaTCalculator deltaTCalculator) {

        List<RoutingContext> routingContexts = new ArrayList<>();
        for (int i = 0; i < scenarios.size(); i++) {
            RoutingContext ctx = new RoutingContext(
                new SwissRailRaptor.Builder(raptorData.get(i), this.scenarios.getFirst().getConfig()).build(),
                raptorParams,
                ctxs.get(i).stopsPerZone(),
                ctxs.get(i).stopLookupPerDestination()
            );
            routingContexts.add(ctx);
        }

        return new BewertoWorker(
            workerQueue, params, demand, routingContexts, destinationZoneIds, deltaTCalculator, demandFactorCalculator
        );
    }

    @Override
    public BewertoWorkItem createWorkItem(String originZone) {

        // The number of variants
        int v = scenarios.size() - 1;

        return new BewertoWorkItem(originZone,
            new CompletableFuture<>(),
            IntStream.of(v).mapToObj(i -> new CompletableFuture<UmlegoWorkResult>()).toList(),
            IntStream.of(v).mapToObj(i -> new CompletableFuture<UmlegoWorkResult>()).toList(),
            IntStream.of(v).mapToObj(i -> new CompletableFuture<BewertoWorkResult>()).toList()
        );
    }

    @Override
    public List<? extends WorkResultHandler<?>> createResultHandler(UmlegoParameters params, String outputFolder, List<String> destinationZoneIds, List<UmlegoListener> listeners) {

        List<WorkResultHandler<?>> handler = new ArrayList<>();

        // Base case results
        handler.add(new ResultWriter(
            outputFolder + "/" + scenarios.getFirst().getConfig().controller().getRunId(),
            scenarios.getFirst().getTransitSchedule(),
            listeners,
            params.writer(),
            destinationZoneIds
        ));

        for (int i = 1; i < scenarios.size(); i++) {

            Scenario s = scenarios.get(i);

            ResultWriter writer = new ResultWriter(
                outputFolder + "/" + s.getConfig().controller().getRunId(),
                s.getTransitSchedule(),
                listeners,
                params.writer(),
                destinationZoneIds
            );
            handler.add(writer);

            String out = outputFolder + "/" + s.getConfig().controller().getRunId() + "_induced";
            ResultWriter writer2 = new ResultWriter(
                out,
                s.getTransitSchedule(),
                listeners,
                params.writer(),
                destinationZoneIds
            );

            handler.add(writer2);
            handler.add(new BewertoResultWriter(out, params.writer().compression(), demand.getZones()));
        }

        return handler;
    }
}
