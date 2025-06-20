package ch.sbb.matsim.umlego;

import ch.sbb.matsim.routing.pt.raptor.*;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.writers.ResultWriter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

/**
 * Factory to create the standard Umlego workflow.
 * This workflow computes all routes and assigns the demand once.
 */
public class UmlegoWorkflowFactory implements WorkflowFactory<UmlegoWorkItem> {

    private final DemandMatrices demand;
    private final Scenario scenario;
    private final RaptorParameters raptorParams;
    private final SwissRailRaptorData raptorData;


    public UmlegoWorkflowFactory(DemandMatrices demand, Scenario scenario) {
        this.demand = demand;
        this.scenario = scenario;

        // prepare SwissRailRaptor
        // TODO: these parameters could be added to a central location.
        raptorParams = RaptorUtils.createParameters(scenario.getConfig());
        raptorParams.setTransferPenaltyFixCostPerTransfer(0.01);
        raptorParams.setTransferPenaltyMinimum(0.01);
        raptorParams.setTransferPenaltyMaximum(0.01);

        RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(scenario.getConfig());
        raptorConfig.setOptimization(RaptorStaticConfig.RaptorOptimization.OneToAllRouting);
        // make sure SwissRailRaptor does not add any more transfers than what is specified in minimal transfer times:
        raptorConfig.setBeelineWalkConnectionDistance(10.0);
        raptorData = SwissRailRaptorData.create(this.scenario.getTransitSchedule(),
                this.scenario.getTransitVehicles(), raptorConfig, this.scenario.getNetwork(), null);
    }

    @Override
    public AbstractWorker<UmlegoWorkItem> createWorker(BlockingQueue<UmlegoWorkItem> workerQueue, UmlegoParameters params, List<String> destinationZoneIds,
                                       Map<String, List<ZoneConnections.ConnectedStop>> stopsPerZone,
                                       Map<String, Map<TransitStopFacility, ZoneConnections.ConnectedStop>> stopLookupPerDestination,
                                       DeltaTCalculator deltaTCalculator) {

        SwissRailRaptor raptor = new SwissRailRaptor.Builder(raptorData, this.scenario.getConfig()).build();
        return new UmlegoWorker(workerQueue, params, demand, raptor, raptorParams,
                destinationZoneIds, stopsPerZone, stopLookupPerDestination, deltaTCalculator);
    }


    @Override
    public UmlegoWorkItem createWorkItem(String originZone) {
        CompletableFuture<UmlegoWorkResult> future = new CompletableFuture<>();
        return new UmlegoWorkItem(originZone, future);
    }

    @Override
    public List<WorkResultHandler<?>> createResultHandler(UmlegoParameters params, String outputFolder, List<String> destinationZoneIds, List<UmlegoListener> listeners) {
        // Umlego workflow has one ResultWriter that writes the results to the output folder.
        return List.of(new ResultWriter(outputFolder, this.scenario.getTransitSchedule(), listeners, params.writer(), destinationZoneIds));
    }
}
