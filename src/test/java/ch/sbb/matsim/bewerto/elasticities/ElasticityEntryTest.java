package ch.sbb.matsim.bewerto.elasticities;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class ElasticityEntryTest {

    @Test
    void testReadAllEntries() throws IOException {
        // Read entries from test file
        List<ElasticityEntry> entries = ElasticityEntry.readAllEntries("src/test/resources/test_Elastizitaeten.csv");
        
        assertEquals(48, entries.size(), "Should read all entries from the test file");
        
        // Test some specific entries to verify correct parsing
        // Test first entry (Cluster 1, Fr, Binnenverkehr, JRT)
        ElasticityEntry firstEntry = entries.stream()
                .filter(e -> e.cluster() == 1 && e.segment().equals("Fr") && e.skimType().equals(SkimType.JRT))
                .findFirst()
                .orElseThrow();
        
        assertEquals(1, firstEntry.cluster());
        assertEquals("Fr", firstEntry.segment());
        assertEquals("Binnenverkehr", firstEntry.description());
        assertEquals(SkimType.JRT, firstEntry.skimType());
        assertEquals(-2.34, firstEntry.elasticity0(), 0.001);
        assertEquals(5.7, firstEntry.a(), 0.001);
        assertEquals(-9.8, firstEntry.b(), 0.001);
        assertEquals(-4.5, firstEntry.min(), 0.001);
        assertEquals(-6.2, firstEntry.max(), 0.001);
        assertEquals(3.1, firstEntry.fMin(), 0.001);
        assertEquals(8.9, firstEntry.fMax(), 0.001);
        assertNull(firstEntry.kgMax(), "JRT entries should not have kg_max");
        
        // Test an ADT entry which should have kg_max
        ElasticityEntry adtEntry = entries.stream()
                .filter(e -> e.cluster() == 1 && e.segment().equals("Fr") && e.skimType().equals(SkimType.ADT))
                .findFirst()
                .orElseThrow();
        
        assertEquals(158.0, adtEntry.kgMax(), 0.001, "ADT entries should have kg_max");
        
        // Test PM entry with large min/max values
        ElasticityEntry pmEntry = entries.stream()
                .filter(e -> e.cluster() == 1 && e.segment().equals("Fr") && e.skimType().equals(SkimType.PM))
                .findFirst()
                .orElseThrow();
        
        assertEquals(-256.0, pmEntry.min(), 0.001);
        assertEquals(189.0, pmEntry.max(), 0.001);
    }
} 