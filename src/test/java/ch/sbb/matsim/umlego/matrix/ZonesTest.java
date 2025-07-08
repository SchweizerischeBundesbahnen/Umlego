package ch.sbb.matsim.umlego.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZonesTest {

    private Zones zones;
    private Zones extendedZones;
    private static final String TEST_ZONES_CSV = "zonesLookup.csv";
    private static final String TEST_EXTENDED_ZONES_CSV = "zonesLookup_Extended.csv";

    @BeforeEach
    void setUp() throws IOException {
        // Get the path to the test resources
        String resourcePath = getClass().getClassLoader().getResource(TEST_ZONES_CSV).getPath();
        String extendedResourcePath = getClass().getClassLoader().getResource(TEST_EXTENDED_ZONES_CSV).getPath();

        zones = new Zones(resourcePath);
        extendedZones = new Zones(extendedResourcePath);
    }

    @Test
    void testSize() {
        // Test size of the lookup
        assertTrue(zones.size() > 0);
        assertTrue(extendedZones.size() > 0);
    }

    @Test
    void testGetAllNos() {
        // Test getting all lookup values
        List<String> values = zones.getAllNos();
        assertNotNull(values);
        assertFalse(values.isEmpty());
        assertTrue(values.contains("1"));
        assertTrue(values.contains("2"));
    }

    @Test
    void testGetCluster() throws ZoneNotFoundException {
        // Test cluster lookup
        assertEquals("CH", extendedZones.getCluster("2377"));
        assertEquals("Ausland", extendedZones.getCluster("2372"));
        assertEquals("GG", extendedZones.getCluster("2373"));
    }

    @Test
    void testConstructorWithList() {
        // Test constructor with Map
        var zone1 = new Zone("Zone1", "1", "Cluster1");
        var zone2 = new Zone("Zone2", "2", "Cluster1");

        Zones customLookup = new Zones(List.of(zone1, zone2));

        assertEquals(2, customLookup.size());
        assertTrue(customLookup.getAllNos().contains("Zone1"));
        assertTrue(customLookup.getAllNos().contains("Zone2"));
    }
}