package ch.sbb.matsim.umlego;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.sbb.matsim.umlego.Connectors.ConnectedStop;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

class ConnectorsTest {

    @Test
    void fillConnectionsWithinWalkingDistance() {

        var schedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
        Table<String, Id<TransitStopFacility>, ConnectedStop> connectionsPerZoneStopPair = HashBasedTable.create();
        var zoneId = "1";
        var stopId = Id.create(1, TransitStopFacility.class);
        var stop = schedule.getFactory().createTransitStopFacility(stopId, new Coord(0, 0), false);
        connectionsPerZoneStopPair.put(zoneId, stopId, new ConnectedStop("", 5 * 60.0, stop));

        var table = Connectors.fillConnectionsWithinWalkingDistance(connectionsPerZoneStopPair, schedule);
        assertEquals(1, table.size());
    }

    @Test
    void fillConnectionsWithinWalkingDistance_2() {

        var schedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
        Table<String, Id<TransitStopFacility>, ConnectedStop> connectionsPerZoneStopPair = HashBasedTable.create();
        var zoneId = "1";
        var stop1 = schedule.getFactory()
            .createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(0, 0), false);
        var stop2 = schedule.getFactory()
            .createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(0, 0), false);

        connectionsPerZoneStopPair.put(zoneId, stop1.getId(), new ConnectedStop("", 5 * 60.0, stop1));
        connectionsPerZoneStopPair.put(zoneId, stop2.getId(), new ConnectedStop("", 5 * 60.0, stop2));

        var table = Connectors.fillConnectionsWithinWalkingDistance(connectionsPerZoneStopPair, schedule);
        assertEquals(2, table.size());
    }

    @Test
    void fillConnectionsWithinWalkingDistance_3() {

        var schedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
        Table<String, Id<TransitStopFacility>, ConnectedStop> connectionsPerZoneStopPair = HashBasedTable.create();
        var zoneId = "1";
        var stop1 = schedule.getFactory()
            .createTransitStopFacility(Id.create(1, TransitStopFacility.class), new Coord(0, 0), false);
        var stop2 = schedule.getFactory()
            .createTransitStopFacility(Id.create(2, TransitStopFacility.class), new Coord(0, 0), false);

        schedule.addStopFacility(stop1);
        schedule.addStopFacility(stop2);

        connectionsPerZoneStopPair.put(zoneId, stop1.getId(), new ConnectedStop("", 5 * 60.0, stop1));

        schedule.getMinimalTransferTimes().set(stop1.getId(), stop2.getId(), 120);

        var table = Connectors.fillConnectionsWithinWalkingDistance(connectionsPerZoneStopPair, schedule);
        assertEquals(2, table.size());
    }

    @Test
    void readZoneConnectors() throws IOException, URISyntaxException {
        String connectionsFile = Paths.get(getClass().getClassLoader().getResource("filledConnections.csv").toURI()).toString();

        TransitSchedule schedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();

        Table<String, Id<TransitStopFacility>, ConnectedStop> connectionTable = Connectors.readZoneConnectors(connectionsFile, schedule);

        // no stop point in transit schedule that matches!
        assertThat(connectionTable.rowKeySet()).hasSize(0);
    }
}