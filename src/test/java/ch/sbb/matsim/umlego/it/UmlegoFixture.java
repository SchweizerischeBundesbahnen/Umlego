package ch.sbb.matsim.umlego.it;

import ch.sbb.matsim.umlego.config.*;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.builder.GestaltBuilder;
import org.github.gestalt.config.exceptions.GestaltException;
import org.github.gestalt.config.source.ClassPathConfigSourceBuilder;
import org.github.gestalt.config.yaml.YamlModuleConfigBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils.ScenarioBuilder;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicles;

import java.util.*;

/*package*/  public class UmlegoFixture {

    /*package*/ final MutableScenario scenario;
    /*package*/ final Config config;
    /*package*/ final Network network;
    /*package*/ final TransitScheduleFactory builder;
    /*package*/ final TransitSchedule schedule;
    /*package*/ final Vehicles transitVehicles;

    public UmlegoFixture() {
        this.config = ConfigUtils.createConfig();
        this.config.transit().setUseTransit(true);

        ScenarioBuilder scBuilder = new ScenarioBuilder(config);
        this.scenario = (MutableScenario) scBuilder.build();

        this.network = this.scenario.getNetwork();
        this.schedule = this.scenario.getTransitSchedule();
        this.builder = this.schedule.getFactory();
        this.transitVehicles = this.scenario.getTransitVehicles();
    }

    public static UmlegoParameters createUmlegoParameters() {
        SearchImpedanceParameters search = new SearchImpedanceParameters(1.0, 1.0, 1.0, 1.0, 1.0, 10.0);
        PreselectionParameters preselection = new PreselectionParameters(2.0, 60.0);
        PerceivedJourneyTimeParameters pjt = new PerceivedJourneyTimeParameters(1.0, 2.94, 2.94, 2.25, 1.13, 17.24, 0.03, 58.0);
        RouteImpedanceParameters impedance = new RouteImpedanceParameters(1.0, 1.85, 1.85);
        RouteSelectionParameters routeSelection = new RouteSelectionParameters(false, 3600.0, 3600.0, new UtilityFunctionParams(UtilityFunctionParams.Type.boxcox, Map.of("beta", 1.536, "tau", 0.5)));
        WriterParameters writer = new WriterParameters(1e-5, Set.of());
        return new UmlegoParameters(5, 1, search, preselection, pjt, impedance, routeSelection, writer);

    }

    public static UmlegoParameters loadTestConfig() throws GestaltException {
        Gestalt config = new GestaltBuilder()
                .addSource(ClassPathConfigSourceBuilder.builder().setResource("/umlego.yaml").build())
                .addSource(ClassPathConfigSourceBuilder.builder().setResource("/test.yaml").build())
                .addModuleConfig(YamlModuleConfigBuilder.builder().build())
                .build();

        config.loadConfigs();

        return config.getConfig("umlego", UmlegoParameters.class);
    }

    protected TransitStopFacility buildStop(String name, double x, double y) {

        var s = this.builder.createTransitStopFacility(Id.create(name, TransitStopFacility.class), new Coord(x, y), true);
        var l = this.buildLink(name);
        s.setLinkId(l.getId());

        this.schedule.addStopFacility(s);
        return s;

    }

    protected Link buildLink(String name) {
        var n1 = this.network.getFactory().createNode(Id.create("from_" + name, Node.class), new Coord((double) 0, (double) 5000));
        var n2 = this.network.getFactory().createNode(Id.create("to_" + name, Node.class), new Coord((double) 0, (double) 5000));
        this.network.addNode(n1);
        this.network.addNode(n2);
        var l = this.network.getFactory().createLink(Id.create(name, Link.class), n1, n2);
        l.setLength(1.);
        this.network.addLink(l);
        return l;
    }

    protected void buildLine(String lineId, List<TransitStopFacility> stopFacilities, List<String> departureOffsets, List<String> departures) {
        var line = this.builder.createTransitLine(Id.create(lineId, TransitLine.class));
        this.schedule.addTransitLine(line);

        var l1 = this.buildLink("from_" + lineId);
        var l2 = this.buildLink("to" + lineId);

        NetworkRoute netRoute = RouteUtils.createLinkNetworkRouteImpl(l1.getId(), l2.getId());
        List<TransitRouteStop> stops = new ArrayList<>();
        var n = stopFacilities.size();
        for (int i = 0; i < n; i++) {
            stops.add(this.builder.createTransitRouteStopBuilder(stopFacilities.get(i)).departureOffset(hhmmToSec(departureOffsets.get(i))).build());
        }

        TransitRoute route = this.builder.createTransitRoute(Id.create("red C > G", TransitRoute.class), netRoute, stops, "rail");
        line.addRoute(route);

        var departureId = 0;
        for (var departure : departures) {
            departureId += 1;
            route.addDeparture(this.builder.createDeparture(Id.create(departureId, Departure.class), hhmmToSec(departure)));
        }

    }

    static double hhmmToSec(String hhmm) {
        var data = Arrays.stream(hhmm.split(":")).toList();
        return Integer.parseInt(data.get(0)) * 60 * 60 + Integer.parseInt(data.get(1)) * 60;
    }
}