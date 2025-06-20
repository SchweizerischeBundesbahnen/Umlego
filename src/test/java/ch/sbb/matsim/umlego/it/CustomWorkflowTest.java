package ch.sbb.matsim.umlego.it;

import ch.sbb.matsim.umlego.*;
import ch.sbb.matsim.umlego.ZoneConnections.ConnectedStop;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import ch.sbb.matsim.umlego.deltat.DeltaTCalculator;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.DemandMatrix;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import static ch.sbb.matsim.umlego.it.UmlegoFixture.createUmlegoParameters;

/**
 * Test for the new architecture with generalized workflow.
 */
public class CustomWorkflowTest {

    private String LAUSANNE;
    private String GENEVE;
    private Map<String, List<ConnectedStop>> stopsPerZone;
    private DemandMatrices demand;

    @BeforeEach
    void setUp() {
        UmlegoFixture fixture = new UmlegoFixture();

        TransitStopFacility lausanne = fixture.buildStop("lausanne", 2532820.15, 1154661.65);
        TransitStopFacility geneve = fixture.buildStop("geneve", 2499812.38, 1118367.70);

        fixture.buildLine("testLine", List.of(lausanne, geneve), List.of("00:00", "01:00"), List.of("05:00"));

        LAUSANNE = "Lausanne";
        GENEVE = "Geneve";

        var data = new HashMap<String, Integer>();
        data.put(GENEVE, 0);
        data.put(LAUSANNE, 1);

        double[][] m = {{10, 10}, {10, 10}};
        var matrix = new DemandMatrix(23 * 60 + 50, 24 * 60, m);
        demand = new DemandMatrices(List.of(matrix), new ZonesLookup(data));

        stopsPerZone = new HashMap<>();
        stopsPerZone.put(GENEVE, List.of(new ConnectedStop(GENEVE, 0, geneve)));
        stopsPerZone.put(LAUSANNE, List.of(new ConnectedStop(LAUSANNE, 0, lausanne)));
    }

    @Test
    void testCustomWorkflow() throws Exception {
        // Create a custom workflow factory
        MockWorkflowFactory mockWorkflowFactory = new MockWorkflowFactory();

        // Initialize Umlego with the custom workflow factory
        Umlego umlego = new Umlego(demand, stopsPerZone, mockWorkflowFactory);

        // Create a test result listener
        TestResultListener listener = new TestResultListener();
        umlego.addListener(listener);

        // Run Umlego with custom workflow
        var params = createUmlegoParameters();

        umlego.run(List.of(LAUSANNE), List.of(GENEVE), params, 1, "");

        // Verify the results
        Assertions.assertTrue(mockWorkflowFactory.createWorkerCalled, "Worker should have been created");
        Assertions.assertTrue(mockWorkflowFactory.createWorkItemCalled, "Work item should have been created");
        Assertions.assertTrue(mockWorkflowFactory.createResultHandlerCalled, "Result handler should have been created");

        // Verify the work items were created with the correct origin zone
        List<MockWorkResult> results = listener.getResults();
        Assertions.assertEquals(2, results.size(), "Should have two results");
        Assertions.assertTrue(results.stream().allMatch(r -> LAUSANNE.equals(r.originZone())),
                "All results should be from Lausanne");
        Assertions.assertTrue(results.stream().anyMatch(r -> "result1".equals(r.resultType())),
                "Should have a result of type 'result1'");
        Assertions.assertTrue(results.stream().anyMatch(r -> "result2".equals(r.resultType())),
                "Should have a result of type 'result2'");

        // Verify the work item was processed
        Assertions.assertTrue(mockWorkflowFactory.workItemProcessed,
                "Work item should have been processed by the worker");
    }

    /**
     * Mock implementation of WorkflowFactory for testing purposes.
     */
    private static class MockWorkflowFactory implements WorkflowFactory<MockWorkItem> {
        boolean createWorkerCalled = false;
        boolean createWorkItemCalled = false;
        boolean createResultHandlerCalled = false;
        boolean workItemProcessed = false;

        @Override
        public AbstractWorker<MockWorkItem> createWorker(BlockingQueue<MockWorkItem> workerQueue, UmlegoParameters params,
                                                           List<String> destinationZoneIds,
                                                           Map<String, List<ConnectedStop>> stopsPerZone,
                                                           Map<String, Map<TransitStopFacility, ConnectedStop>> stopLookupPerDestination,
                                                           DeltaTCalculator deltaTCalculator) {
            createWorkerCalled = true;
            return new AbstractWorker<>(workerQueue) {
                @Override
                protected void processOriginZone(MockWorkItem workItem) {
                    workItemProcessed = true;

                    for (int i = 0; i < workItem.results().size(); i++) {
                        CompletableFuture<WorkResult> future = (CompletableFuture<WorkResult>) workItem.results().get(i);
                        if (!future.isDone()) {
                            future.complete(new MockWorkResult(workItem.originZone(), i == 0 ? "result1" : "result2"));
                        }
                    }
                }
            };
        }

        @Override
        public MockWorkItem createWorkItem(String originZone) {
            createWorkItemCalled = true;
            CompletableFuture<MockWorkResult> future1 = new CompletableFuture<>();
            CompletableFuture<MockWorkResult> future2 = new CompletableFuture<>();
            return new MockWorkItem(originZone, List.of(future1, future2));
        }

        @Override
        public List<WorkResultHandler<?>> createResultHandler(UmlegoParameters params, String outputFolder,
                                                              List<String> destinationZoneIds,
                                                              List<UmlegoListener> listeners) {
            createResultHandlerCalled = true;
            return List.of(new MockResultHandler(listeners), new MockResultHandler(listeners));
        }
    }

    private record MockWorkItem(String originZone, List<CompletableFuture<? extends WorkResult>> results) implements WorkItem {
    }

    /**
     * Mock implementation of WorkResult for testing purposes.
     */
    private record MockWorkResult(String originZone, String resultType) implements WorkResult {
    }

    /**
     * Mock implementation of WorkResultHandler for testing purposes.
     */
    private record MockResultHandler(List<UmlegoListener> listeners) implements WorkResultHandler<MockWorkResult> {

        @Override
        public void handleResult(MockWorkResult result) {
            for (UmlegoListener listener : listeners) {
                if (listener instanceof TestResultListener testListener) {
                    testListener.receiveResult(result);
                }
            }
        }
    }

    /**
     * Test listener to verify the workflow results.
     */
    private static class TestResultListener implements UmlegoListener {
        private final List<MockWorkResult> results = new ArrayList<>();

        public void receiveResult(MockWorkResult result) {
            results.add(result);
        }

        public List<MockWorkResult> getResults() {
            return results;
        }

        @Override
        public void processRoute(String originZone, String destinationZone, FoundRoute route) {
            // Not used in these tests
        }
    }
}
