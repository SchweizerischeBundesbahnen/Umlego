package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.routing.pt.raptor.*;
import ch.sbb.matsim.umlego.*;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.writers.ResultWriter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

/**
 * Factory for the bewerto workflow.
 *
 * The workflow processed multiple scenarios at once, computing the routes and assigning the demand once for all scenarios.
 */
public class BewertoWorkflowFactory implements WorkflowFactory<BewertoWorkItem> {

    private final DemandMatrices demand;
    private final RaptorParameters raptorParams;

    private final List<Scenario> scenarios;
    private final List<SwissRailRaptorData> raptorData = new ArrayList<>();

    public BewertoWorkflowFactory(DemandMatrices demand, Scenario baseCase, List<Scenario> variants) {
        this.demand = demand;
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
    public AbstractWorker<BewertoWorkItem> createWorker(BlockingQueue<BewertoWorkItem> workerQueue, UmlegoParameters params, List<String> destinationZoneIds, Map<String, List<ZoneConnections.ConnectedStop>> stopsPerZone, Map<String, Map<TransitStopFacility, ZoneConnections.ConnectedStop>> stopLookupPerDestination, DeltaTCalculator deltaTCalculator) {
        return new BewertoWorker(
                workerQueue, params, demand,
                raptorData.stream().map(r -> new SwissRailRaptor.Builder(r, this.scenarios.getFirst().getConfig()).build()).toList(),
                raptorParams, destinationZoneIds, stopsPerZone, stopLookupPerDestination, deltaTCalculator
        );
    }

    @Override
    public BewertoWorkItem createWorkItem(String originZone) {
        return new BewertoWorkItem(originZone,
                scenarios.stream().map(s -> new CompletableFuture<UmlegoWorkResult>()).toList()
        );
    }

    @Override
    public List<? extends WorkResultHandler<?>> createResultHandler(UmlegoParameters params, String outputFolder, List<String> destinationZoneIds, List<UmlegoListener> listeners) {

        // TODO: there are multiple results (base/induced demand) per variant also

        return scenarios.stream().map(
                s -> new ResultWriter(
                        outputFolder + "/" + s.getConfig().controller().getRunId(),
                        s.getTransitSchedule(),
                        listeners,
                        params.writer(),
                        destinationZoneIds
                )
        ).toList();
    }
}
