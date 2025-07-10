package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.bewerto.config.BewertoParameters;
import ch.sbb.matsim.bewerto.elasticities.DemandFactorCalculator;
import ch.sbb.matsim.routing.pt.raptor.*;
import ch.sbb.matsim.umlego.*;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.writers.ResultWriter;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.IOException;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

/**
 * Factory for the bewerto workflow.
 * <p>
 * The workflow processed multiple scenarios at once, computing the routes and assigning the demand once for all scenarios.
 */
public class BewertoWorkflowFactory implements WorkflowFactory<BewertoWorkItem> {

    private final DemandMatrices demand;
    private final DemandFactorCalculator demandFactorCalculator;
    private final String zoneConnectionsFile;

    private final RaptorParameters raptorParams;
    private final List<Scenario> scenarios;

    /**
     * Save prepared routing contexts for each scenario.
     */
    private final List<RoutingContext> ctxs = new ArrayList<>();
    private final List<SwissRailRaptorData> raptorData = new ArrayList<>();

    public BewertoWorkflowFactory(BewertoParameters parameters, DemandMatrices demand, String zoneConnectionsFile,
                                  Scenario baseCase, List<Scenario> variants) {
        this.demand = demand;
        this.zoneConnectionsFile = zoneConnectionsFile;
        this.demandFactorCalculator = new DemandFactorCalculator(parameters.getElasticities(), demand.getZones());
        this.scenarios = new ArrayList<>(variants);
        this.scenarios.addFirst(baseCase);

        // prepare SwissRailRaptor
        // TODO: these parameters could be added to a central location.
        raptorParams = RaptorUtils.createParameters(scenarios.getFirst().getConfig());
        raptorParams.setTransferPenaltyFixCostPerTransfer(0.01);
        raptorParams.setTransferPenaltyMinimum(0.01);
        raptorParams.setTransferPenaltyMaximum(0.01);

        RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(scenarios.getFirst().getConfig());
        raptorConfig.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
        // make sure SwissRailRaptor does not add any more transfers than what is specified in minimal transfer times:
        raptorConfig.setBeelineWalkConnectionDistance(10.0);

        for (Scenario scenario : scenarios) {
            SwissRailRaptorData d = SwissRailRaptorData.create(scenario.getTransitSchedule(), scenario.getTransitVehicles(),
                    raptorConfig, scenario.getNetwork(), null);

            raptorData.add(d);
        }
    }

    @Override
    public IntSet computeDestinationStopIndices(List<String> destinationZoneIds) throws IOException {

        for (Scenario scenario : scenarios) {
            Map<String, List<Connectors.ConnectedStop>> stopsPerZone = UmlegoRunner.readConnectors(zoneConnectionsFile, scenario.getTransitSchedule());
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
