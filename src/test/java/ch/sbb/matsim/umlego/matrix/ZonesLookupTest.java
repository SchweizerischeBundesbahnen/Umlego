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

class ZonesLookupTest {

    private ZonesLookup zonesLookup;
    private ZonesLookup extendedZonesLookup;
    private static final String TEST_ZONES_CSV = "zonesLookup.csv";
    private static final String TEST_EXTENDED_ZONES_CSV = "zonesLookup_Extended.csv";

    @BeforeEach
    void setUp() throws IOException {
        // Get the path to the test resources
        String resourcePath = getClass().getClassLoader().getResource(TEST_ZONES_CSV).getPath();
        String extendedResourcePath = getClass().getClassLoader().getResource(TEST_EXTENDED_ZONES_CSV).getPath();
        
        zonesLookup = new ZonesLookup(resourcePath);
        extendedZonesLookup = new ZonesLookup(extendedResourcePath);
    }

    @Test
    void testBasicZoneLookup() throws ZoneNotFoundException {
        // Test basic zone lookup
        assertEquals(0, zonesLookup.getIndex("1"));
        assertEquals(1, zonesLookup.getIndex("2"));
        assertEquals("1", zonesLookup.getZone(0));
        assertEquals("2", zonesLookup.getZone(1));
    }

    @Test
    void testExtendedZoneLookup() throws ZoneNotFoundException {
        // Test extended zone lookup with more fields
        assertEquals(0, extendedZonesLookup.getIndex("1"));
        assertEquals(2372, extendedZonesLookup.getIndex("7420"));
        assertEquals("1", extendedZonesLookup.getZone(0));
        assertEquals("7420", extendedZonesLookup.getZone(2372));
    }

    @Test
    void testNonExistentZone() {
        // Test handling of non-existent zone
        assertThrows(ZoneNotFoundException.class, () -> zonesLookup.getIndex("NonExistentZone"));
    }

    @Test
    void testInvalidIndex() {
        // Test handling of invalid index
        assertThrows(IllegalArgumentException.class, () -> zonesLookup.getZone(-1));
        assertThrows(IllegalArgumentException.class, () -> zonesLookup.getZone(10000));
    }

    @Test
    void testSize() {
        // Test size of the lookup
        assertTrue(zonesLookup.size() > 0);
        assertTrue(extendedZonesLookup.size() > 0);
    }

    @Test
    void testGetAllLookupValues() {
        // Test getting all lookup values
        List<String> values = zonesLookup.getAllLookupValues();
        assertNotNull(values);
        assertFalse(values.isEmpty());
        assertTrue(values.contains("1"));
        assertTrue(values.contains("2"));
    }

    @Test
    void testGetIndexWithInvalidZones() throws ZoneNotFoundException {
        // Test getIndex with invalid zones handling
        Set<String> invalidZoneIds = new HashSet<>();
        int index = zonesLookup.getIndex("NonExistentZone", invalidZoneIds, true);
        assertEquals(-1, index);
        assertTrue(invalidZoneIds.contains("NonExistentZone"));
    }

    @Test
    void testGetCluster() throws ZoneNotFoundException {
        // Test cluster lookup
        assertEquals("CH", extendedZonesLookup.getCluster("1"));
        assertEquals("Ausland", extendedZonesLookup.getCluster("7420"));
        assertEquals("GG", extendedZonesLookup.getCluster("7421"));
    }

    @Test
    void testConstructorWithMap() {
        // Test constructor with Map
        Map<String, Integer> testMap = Map.of("Zone1", 1, "Zone2", 2);
        ZonesLookup customLookup = new ZonesLookup(testMap);
        
        assertEquals(2, customLookup.size());
        assertTrue(customLookup.getAllLookupValues().contains("Zone1"));
        assertTrue(customLookup.getAllLookupValues().contains("Zone2"));
    }
}