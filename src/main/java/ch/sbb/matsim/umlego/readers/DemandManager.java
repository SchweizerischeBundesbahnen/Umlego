package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.FactorMatrix;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;

import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The DemandManager provides methods for
 * <li>reading demand matrices and</li>
 * <li>reading and applying correction factor matrices.</li>
 */
public final class DemandManager {

    private static final Logger LOG = LogManager.getLogger(DemandManager.class);

    private DemandManager() {}

    /**
     * Static method to prepare demand matrices by loading the base demand matrices
     * and applying correction factors based on provided files.
     *
     * @param zonesFile the path to the zones file used for lookup
     * @param demandMatricesPath the path to the base demand matrices
     * @param factorMatriceFilenames paths to correction factor matrix files
     * @return DemandMatrices representing the prepared demand matrices
     * @throws IOException if an I/O error occurs during loading or applying correction factors
     * @throws ZoneNotFoundException if a zone is not found in the lookup
     */
    public static DemandMatrices prepareDemand(String zonesFile, String demandMatricesPath, String... factorMatriceFilenames) throws IOException, ZoneNotFoundException {
        DemandManager demandManager = new DemandManager();
        return demandManager.execute(zonesFile, demandMatricesPath, null, null, factorMatriceFilenames);
    }

    /**
     * Same as above, but with params:
     *
     * @param simbaRunId the identifier for the specified Simba-Run
     * @param saison the season identifier used for matrix parsing
     */
    public static DemandMatrices prepareDemand(String zonesFile, String simbaRunId, String saison, String... factorMatriceFilenames) throws IOException, ZoneNotFoundException {
        DemandManager demandManager = new DemandManager();
        return demandManager.execute(zonesFile, null, simbaRunId, saison, factorMatriceFilenames);
    }

    /**
     * Helper method to prepare demand matrices by loading base matrices
     * and applying correction factors based on provided file paths.
     *
     * @param zonesFile the path to the zones file used for lookup
     * @param demandMatricesPath the path to the base demand matrices
     * @param factorMatriceFilenames paths to correction factor matrix files
     * @return DemandMatrices representing the prepared demand matrices
     * @throws IOException if an I/O error occurs during loading or applying correction factors
     * @throws ZoneNotFoundException if a zone is not found in the lookup
     */
    private DemandMatrices execute(String zonesFile, String demandMatricesPath, String simbaRunId, String saison, String... factorMatriceFilenames) throws IOException, ZoneNotFoundException {
        ZonesLookup zonesLookup = loadZoneLookupFile(zonesFile);
        DemandMatrices demandMatrices = loadDemandMatrices(demandMatricesPath, simbaRunId, saison, zonesLookup);
        loadAndApplyCorrectionFactors(demandMatrices, factorMatriceFilenames);
        return demandMatrices;
    }

    private ZonesLookup loadZoneLookupFile(String zonesFile) {
        // Reading and parsing the file is done in the constructor of ZonesLookup :(
        return new ZonesLookup(zonesFile);
    }

    private DemandMatrices loadDemandMatrices(String baseMatricesPath, String simbaRunId, String saison, ZonesLookup zonesLookup) throws IOException, ZoneNotFoundException {
        DemandMatricesParser parser = DemandMatricesParserFactory.createParser(baseMatricesPath, simbaRunId, saison, zonesLookup);
        return parser.parse();
    }

    /**
     * Applies correction factors to the demand matrices by parsing each factor matrix file
     * and multiplying the demand matrices with the parsed factors.
     *
     * @param demandMatrices the demand matrices to which correction factors are applied
     * @param factorMatriceFilenames paths to the correction factor matrix files
     * @throws IOException if an I/O error occurs during parsing
     * @throws ZoneNotFoundException if a zone is not found in the lookup
     */
    private void loadAndApplyCorrectionFactors(DemandMatrices demandMatrices, String... factorMatriceFilenames) throws IOException, ZoneNotFoundException {
        for (String filename : factorMatriceFilenames) {
            LOG.info("Applying correction factors from {}", filename);
            CsvFactorMatrixParser parser = new CsvFactorMatrixParser(filename, demandMatrices.getLookup(), 1, "\\s+");
            FactorMatrix factorMatrix = parser.parseFactorMatrix();
            demandMatrices.multiplyWith(factorMatrix);
        }
    }
}
