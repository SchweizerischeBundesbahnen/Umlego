package ch.sbb.matsim.umlego.matrix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZonesLookupTest {

    private Zones zones;
    private ZonesLookup zonesLookup;
    private static final String TEST_ZONES_CSV = "zonesLookup_Extended.csv";

    @BeforeEach
    void setUp() throws IOException {
        // Get the path to the test resources
        String resourcePath = getClass().getClassLoader().getResource(TEST_ZONES_CSV).getPath();

        zones = new Zones(resourcePath);
        zonesLookup = zones.createDefaultZonesLookup();
    }

    @Test
    void testBasicZoneLookup() throws ZoneNotFoundException {
        // Test basic zone lookup
        var lookup = zones.createDefaultZonesLookup();
        assertEquals(0, lookup.getIndex("0"));
        assertEquals(1, lookup.getIndex("2372"));
        assertEquals(2, lookup.getIndex("2373"));
        assertEquals(3, lookup.getIndex("2374"));
    }

    @Test
    void testInvalidIndex() {
        // Test handling of invalid index
        assertThrows(ZoneNotFoundException.class, () -> zonesLookup.getIndex("-1"));
        assertThrows(ZoneNotFoundException.class, () -> zonesLookup.getIndex("10000"));
    }

}