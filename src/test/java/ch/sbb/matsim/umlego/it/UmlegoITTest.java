package ch.sbb.matsim.umlego.it;

import ch.sbb.matsim.umlego.RouteUtilityCalculators;
import ch.sbb.matsim.umlego.Umlego;
import ch.sbb.matsim.umlego.Umlego.PerceivedJourneyTimeParameters;
import ch.sbb.matsim.umlego.Umlego.PreselectionParameters;
import ch.sbb.matsim.umlego.Umlego.RouteImpedanceParameters;
import ch.sbb.matsim.umlego.Umlego.RouteSelectionParameters;
import ch.sbb.matsim.umlego.Umlego.SearchImpedanceParameters;
import ch.sbb.matsim.umlego.Umlego.UmlegoParameters;
import ch.sbb.matsim.umlego.Umlego.WriterParameters;
import ch.sbb.matsim.umlego.ZoneConnections.ConnectedStop;
import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.DemandMatrix;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class UmlegoITTest {

    UmlegoParameters createUmlegoParameters() {
        SearchImpedanceParameters search = new SearchImpedanceParameters(1.0, 1.0, 1.0, 1.0, 1.0, 10.0);
        PreselectionParameters preselection = new PreselectionParameters(2.0, 60.0);
        PerceivedJourneyTimeParameters pjt = new PerceivedJourneyTimeParameters(1.0, 2.94, 2.94, 2.25, 1.13, 17.24, 0.03, 58.0);
        RouteImpedanceParameters impedance = new RouteImpedanceParameters(1.0, 1.85, 1.85);
        RouteSelectionParameters routeSelection = new RouteSelectionParameters(false, 3600.0, 3600.0, RouteUtilityCalculators.boxcox(1.536, 0.5));
        WriterParameters writer = new WriterParameters(1e-5, Set.of(), null, new ArrayList<>());
        return new UmlegoParameters(5, search, preselection, pjt, impedance, routeSelection, writer);

    }

    private TransitStopFacility getStop(Scenario scenario, String stopId) {
        return scenario.getTransitSchedule().getFacilities().get(Id.create(stopId, TransitStopFacility.class));
    }

    @Test
    void testRun() throws ZoneNotFoundException {
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

        var data = new HashMap<String, Integer>();
        data.put(GENEVE, 0);
        data.put(LAUSANNE, 1);

        final DemandMatrices demand = new DemandMatrices(List.of(matrix), new ZonesLookup(data));

        Map<String, List<ConnectedStop>> stopsPerZone = new HashMap<>();
        stopsPerZone.put(GENEVE, List.of(
            new ConnectedStop(GENEVE,0, geneve)
            //new ConnectedStop(0, geneveSP)
        ));
        stopsPerZone.put(LAUSANNE, List.of(new ConnectedStop(LAUSANNE,0, lausanne)));

        scenario.getTransitSchedule().getMinimalTransferTimes().set(geneve.getId(), geneveSP.getId(), 0 * 60);
        scenario.getTransitSchedule().getMinimalTransferTimes().set(geneveSP.getId(), geneve.getId(), 0 * 60);
        scenario.getTransitSchedule().getMinimalTransferTimes().set(morges.getId(), morges.getId(), 0 * 60);

        var umlego = new Umlego(demand, scenario, stopsPerZone);
        var params = createUmlegoParameters();

        var listener = new UmlegoITListener(LAUSANNE, GENEVE);
        params.writer().listeners().add(listener);

        umlego.run(params, 1, "", null, null);

        for (var route : listener.routes) {
            var d = route.demand.getOrDefault(GENEVE, 0.);
            var apz = route.adaptationTime.getOrDefault(GENEVE, 0.);
            var lines = route.routeParts.stream().map(rp -> rp.line).filter(Objects::nonNull).map(TransitLine::getId).map(Object::toString).toList();
            System.out.println("\t" + lines + " \t|  #transfer = " + route.transfers + "  \t| demand =" + +d + "\t" + apz / d / 60.0);

        }

        //Assertions.assertEquals(4, listener.routes.size());
        //var routeNames = listener.routes.stream().map(r -> r.routeParts).toList();
        //Assertions.assertEquals(List.of(List.of("red2"), List.of("blue", "green")), routeNames);

    }

}