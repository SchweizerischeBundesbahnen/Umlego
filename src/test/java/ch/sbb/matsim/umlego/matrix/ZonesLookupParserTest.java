package ch.sbb.matsim.umlego.matrix;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ZonesLookupParserTest {

    @Test
    void parseZones() {
        System.setProperty("LOCAL", "true");
        String zonesFile = getClass().getClassLoader().getResource("").getPath() + "zonesLookup.csv";
        ZonesLookupParser parser = new ZonesLookupParser(zonesFile, ";");

        Map<String, Integer> zoneMap = parser.parseZones();

        assertThat(zoneMap.keySet()).hasSize(2571);
    }
}