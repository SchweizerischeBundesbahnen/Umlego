package ch.sbb.matsim.umlego.matrix;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
    void testBasicZoneLookup() throws ZoneNotFoundException {
        // Test basic zone lookup
        assertEquals(0, zones.getNo("1"));
        assertEquals(1, zones.getNo("2"));
        assertEquals("1", zones.getZone(0));
        assertEquals("2", zones.getZone(1));
    }

    @Test
    void testExtendedZoneLookup() throws ZoneNotFoundException {
        // Test extended zone lookup with more fields
        assertEquals(0, extendedZones.getNo("1"));
        assertEquals(2372, extendedZones.getNo("7420"));
        assertEquals("1", extendedZones.getZone(0));
        assertEquals("7420", extendedZones.getZone(2372));
    }

    @Test
    void testNonExistentZone() {
        // Test handling of non-existent zone
        assertThrows(ZoneNotFoundException.class, () -> zones.getNo("NonExistentZone"));
    }

    @Test
    void testInvalidIndex() {
        // Test handling of invalid index
        assertThrows(IllegalArgumentException.class, () -> zones.getZone(-1));
        assertThrows(IllegalArgumentException.class, () -> zones.getZone(10000));
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
    void testGetIndexWithInvalidZones() throws ZoneNotFoundException {
        // Test getIndex with invalid zones handling
        Set<String> invalidZoneIds = new HashSet<>();
        int index = zones.getIndex("NonExistentZone", invalidZoneIds, true);
        assertEquals(-1, index);
        assertTrue(invalidZoneIds.contains("NonExistentZone"));
    }

    @Test
    void testGetCluster() throws ZoneNotFoundException {
        // Test cluster lookup
        assertEquals("CH", extendedZones.getCluster("1"));
        assertEquals("Ausland", extendedZones.getCluster("7420"));
        assertEquals("GG", extendedZones.getCluster("7421"));
    }

    @Test
    void testConstructorWithMap() {
        // Test constructor with Map
        Map<String, Integer> testMap = Map.of("Zone1", 1, "Zone2", 2);
        Zones customLookup = new Zones(testMap);
        
        assertEquals(2, customLookup.size());
        assertTrue(customLookup.getAllNos().contains("Zone1"));
        assertTrue(customLookup.getAllNos().contains("Zone2"));
    }
}