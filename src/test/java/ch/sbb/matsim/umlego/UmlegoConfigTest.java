package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.config.UmlegoParameters;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.exceptions.GestaltException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class UmlegoConfigTest {

    @Test
    public void testLoadConfigWithOverrides() throws GestaltException {
        // Create a path to the test config file
        Path testConfigPath = Paths.get("src/test/resources/test.yaml");
        
        // Load config with overrides from test.yaml
        Gestalt config = UmlegoUtils.loadConfig(testConfigPath);
        UmlegoParameters params = config.getConfig("umlego", UmlegoParameters.class);
        
        // Verify overridden values
        assertEquals(3, params.maxTransfers()); // Overridden in test.yaml
        assertEquals(4, params.threads()); // Overridden in test.yaml
        assertEquals(2.0, params.search().betaInVehicleTime()); // Overridden in test.yaml
        assertEquals(2.0, params.search().betaAccessTime()); // Overridden in test.yaml
        
        // Verify non-overridden values remain as defaults
        assertEquals(1.0, params.search().betaEgressTime());
        assertEquals(1.0, params.search().betaWalkTime());
        assertEquals(1.0, params.search().betaTransferWaitTime());
        assertEquals(10.0, params.search().betaTransferCount());
        
        // Verify other parameters remain default
        assertEquals(2.0, params.preselection().betaMinImpedance());
        assertEquals(60.0, params.preselection().constImpedance());
        
        // Verify PJT parameters remain default
        assertEquals(1.0, params.pjt().betaInVehicleTime());
        assertEquals(2.94, params.pjt().betaAccessTime());
        assertEquals(2.94, params.pjt().betaEgressTime());
        assertEquals(2.25, params.pjt().betaWalkTime());
        assertEquals(1.13, params.pjt().betaTransferWaitTime());
        assertEquals(17.236, params.pjt().transferFix(), 0.001);
        assertEquals(0.033, params.pjt().transferTraveltimeFactor(), 0.001);
        assertEquals(58.0, params.pjt().secondsPerAdditionalStop());

        assertEquals(LocalTime.of(5,0), params.skims().startTime());
        assertEquals(LocalTime.of(22,0), params.skims().endTime());
    }
}
