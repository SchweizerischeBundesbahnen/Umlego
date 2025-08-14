package ch.sbb.matsim.umlego.it;

import static ch.sbb.matsim.umlego.it.UmlegoFixture.createUmlegoParameters;

import ch.sbb.matsim.umlego.Connectors.ConnectedStop;
import ch.sbb.matsim.umlego.Umlego;
import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.matrix.DemandMatrix;
import ch.sbb.matsim.umlego.matrix.Zone;
import ch.sbb.matsim.umlego.matrix.Zones;
import ch.sbb.matsim.umlego.workflows.assignment.AssignmentWorkflowFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class UmlegoITTest {

    private TransitStopFacility getStop(Scenario scenario, String stopId) {
        return scenario.getTransitSchedule().getFacilities().get(Id.create(stopId, TransitStopFacility.class));
    }

    @Test
    void testRun() throws Exception {
        double[][] m = {{10, 10}, {10, 10}};
        var matrix = new DemandMatrix(23 * 60 + 50, 24 * 60, m);

        var fixture = new UmlegoFixture();

        var paris = fixture.buildStop("paris", 398565.13, 1356776.53);
        var geneveSP = fixture.buildStop("geneve secteur france", 2499524.66, 1118330.97);
        var geneve = fixture.buildStop("geneve", 2499812.38, 1118367.70);
        var lausanne = fixture.buildStop("lausanne", 2532820.15, 1154661.65);
        var morges = fixture.buildStop("morges", 2526657.37, 1150360.32);

        fixture.buildLine("livio", List.of(lausanne, morges, geneve), List.of("00:00", "00:30", "01:00"), List.of("05:00"));
        //fixture.buildLine("davi", List.of(lausanne, morges, geneve), List.of("00:00", "00:30", "01:00"), List.of("05:03"));
        //fixture.buildLine("marcus", List.of(lausanne, morges, geneve), List.of("00:00", "00:30", "01:00"), List.of("05:07"));
        //fixture.buildLine("blue", List.of(lausanne, morges), List.of("00:00", "00:30"), List.of("05:30"));
        //fixture.buildLine("green", List.of(morges, geneve), List.of("00:00", "00:30"), List.of("06:00"));
        fixture.buildLine("tgv", List.of(geneveSP, paris), List.of("00:00", "05:30"), List.of("07:00"));

        var scenario = fixture.scenario;

        String LAUSANNE = "Lausanne";
        String GENEVE = "Geneve";

        var geneveZone = new Zone("1", "Geneve", "CH");
        var lausanneZone = new Zone("2", "Lausanne", "CH");

        var zones = new Zones(List.of(geneveZone, lausanneZone));
        var zonesLookup = zones.createDefaultZonesLookup();

        final Matrices demand = new Matrices(List.of(matrix), zones, zonesLookup);

        Map<String, List<ConnectedStop>> stopsPerZone = new HashMap<>();
        stopsPerZone.put(GENEVE, List.of(
            new ConnectedStop(GENEVE, 0, geneve)
            //new ConnectedStop(0, geneveSP)
        ));
        stopsPerZone.put(LAUSANNE, List.of(new ConnectedStop(LAUSANNE, 0, lausanne)));

        scenario.getTransitSchedule().getMinimalTransferTimes().set(geneve.getId(), geneveSP.getId(), 0 * 60);
        scenario.getTransitSchedule().getMinimalTransferTimes().set(geneveSP.getId(), geneve.getId(), 0 * 60);
        scenario.getTransitSchedule().getMinimalTransferTimes().set(morges.getId(), morges.getId(), 0 * 60);

        var umlego = new Umlego(demand, new AssignmentWorkflowFactory(demand, stopsPerZone, scenario));
        var params = createUmlegoParameters();

        var listener = new UmlegoITListener(LAUSANNE, GENEVE);

        umlego.addListener(listener);

        umlego.run(params, 1, "");

        for (var route : listener.routes) {
            var d = route.demand;
            var apz = route.adaptationTime;
            var lines = route.stop2stopRoute.routeParts.stream().map(rp -> rp.line).filter(Objects::nonNull).map(TransitLine::getId).map(Object::toString).toList();
            System.out.println("\t" + lines + " \t|  #transfer = " + route.stop2stopRoute.transfers + "  \t| demand =" + +d + "\t" + apz / d / 60.0);

        }

        //Assertions.assertEquals(4, listener.routes.size());
        //var routeNames = listener.routes.stream().map(r -> r.routeParts).toList();
        //Assertions.assertEquals(List.of(List.of("red2"), List.of("blue", "green")), routeNames);

    }

    /**
     * Test similar to Bewerto's functionality, where a scenario is run first with a base demand and then with an updated demand (in this case halved). Verifies that when the demand is halved, the
     * resulting routes have half the demand.
     */
    @Test
    void testRunWithUpdatedDemand() throws Exception {
        // Create the base demand matrix
        double[][] baseMatrix = {{10, 10}, {10, 10}};
        var baseDemandMatrix = new DemandMatrix(23 * 60 + 50, 24 * 60, baseMatrix);

        // Setup the test scenario with stops and lines
        var fixture = new UmlegoFixture();
        var paris = fixture.buildStop("paris", 398565.13, 1356776.53);
        var geneveSP = fixture.buildStop("geneve secteur france", 2499524.66, 1118330.97);
        var geneve = fixture.buildStop("geneve", 2499812.38, 1118367.70);
        var lausanne = fixture.buildStop("lausanne", 2532820.15, 1154661.65);
        var morges = fixture.buildStop("morges", 2526657.37, 1150360.32);

        fixture.buildLine("livio", List.of(lausanne, morges, geneve), List.of("00:00", "00:30", "01:00"), List.of("05:00"));
        fixture.buildLine("tgv", List.of(geneveSP, paris), List.of("00:00", "05:30"), List.of("07:00"));

        var scenario = fixture.scenario;

        // Define zone names
        String LAUSANNE = "Lausanne";
        String GENEVE = "Geneve";

        var geneveZone = new Zone("1", "Geneve", "CH");
        var lausanneZone = new Zone("2", "Lausanne", "CH");

        var zones = new Zones(List.of(geneveZone, lausanneZone));
        var zonesLookup = zones.createDefaultZonesLookup();

        // Create base demand matrices
        final Matrices baseDemand = new Matrices(List.of(baseDemandMatrix), zones, zonesLookup);

        // Setup zone connections
        Map<String, List<ConnectedStop>> stopsPerZone = new HashMap<>();
        stopsPerZone.put(GENEVE, List.of(new ConnectedStop(GENEVE, 0, geneve)));
        stopsPerZone.put(LAUSANNE, List.of(new ConnectedStop(LAUSANNE, 0, lausanne)));

        // Set transfer times
        scenario.getTransitSchedule().getMinimalTransferTimes().set(geneve.getId(), geneveSP.getId(), 0 * 60);
        scenario.getTransitSchedule().getMinimalTransferTimes().set(geneveSP.getId(), geneve.getId(), 0 * 60);
        scenario.getTransitSchedule().getMinimalTransferTimes().set(morges.getId(), morges.getId(), 0 * 60);

        // Run base case
        var baseUmlego = new Umlego(baseDemand, new AssignmentWorkflowFactory(baseDemand, stopsPerZone, scenario));
        var params = createUmlegoParameters();
        var baseListener = new UmlegoITListener(LAUSANNE, GENEVE);
        baseUmlego.addListener(baseListener);
        baseUmlego.run(params, 1, "");

        double totalDemand = baseListener.routes.stream().mapToDouble(r -> r.demand).sum();

        // Create the halved demand matrix
        double[][] halfMatrix = {{5, 5}, {5, 5}}; // Halved values
        var halfDemandMatrix = new DemandMatrix(23 * 60 + 50, 24 * 60, halfMatrix);
        final Matrices halfDemand = new Matrices(List.of(halfDemandMatrix), zones, zonesLookup);

        // Run with half demand
        var halfUmlego = new Umlego(halfDemand, new AssignmentWorkflowFactory(halfDemand, stopsPerZone, scenario));
        var halfListener = new UmlegoITListener(LAUSANNE, GENEVE);
        halfUmlego.addListener(halfListener);
        halfUmlego.run(params, 1, "");

        // Calculate total demand for the halved case
        double halfDemandTotal = halfListener.routes.stream().mapToDouble(r -> r.demand).sum();

        // Verify that the total demand is halved
        Assertions.assertEquals(totalDemand / 2, halfDemandTotal, 1e-5);

    }

}