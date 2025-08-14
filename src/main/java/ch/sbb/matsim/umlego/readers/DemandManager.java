package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.FactorMatrix;
import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.Zones;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The DemandManager provides methods for
 * <li>reading demand matrices and</li>
 * <li>reading and applying correction factor matrices.</li>
 */
public final class DemandManager {

    private static final Logger LOG = LogManager.getLogger(DemandManager.class);

    private DemandManager() {
    }

    /**
     * Static method to prepare demand matrices by loading the base demand matrices and applying correction factors based on provided files.
     *
     * @param zonesFile the path to the zones file used for lookup
     * @param demandMatricesPath the path to the base demand matrices
     * @param factorMatriceFilenames paths to correction factor matrix files
     * @return DemandMatrices representing the prepared demand matrices
     * @throws IOException if an I/O error occurs during loading or applying correction factors
     * @throws ZoneNotFoundException if a zone is not found in the lookup
     */
    public static Matrices prepareDemand(String zonesFile, String demandMatricesPath, MatrixFactory matrixFactory, String... factorMatriceFilenames) throws IOException, ZoneNotFoundException {
        DemandManager demandManager = new DemandManager();
        return demandManager.execute(zonesFile, demandMatricesPath, matrixFactory, factorMatriceFilenames);
    }

    /**
     * Helper method to prepare demand matrices by loading base matrices and applying correction factors based on provided file paths.
     *
     * @param zonesFile the path to the zones file used for lookup
     * @param demandMatricesPath the path to the base demand matrices
     * @param factorMatriceFilenames paths to correction factor matrix files
     * @return DemandMatrices representing the prepared demand matrices
     * @throws IOException if an I/O error occurs during loading or applying correction factors
     * @throws ZoneNotFoundException if a zone is not found in the lookup
     */
    private Matrices execute(String zonesFile, String demandMatricesPath, MatrixFactory matrixFactory, String... factorMatriceFilenames) throws IOException, ZoneNotFoundException {
        Zones zones = loadZoneLookupFile(zonesFile);
        Matrices matrices = loadDemandMatrices(demandMatricesPath, zones, matrixFactory);
        loadAndApplyCorrectionFactors(matrices, factorMatriceFilenames);
        return matrices;
    }

    private Zones loadZoneLookupFile(String zonesFile) {
        // Reading and parsing the file is done in the constructor of ZonesLookup :(
        try {
            return new Zones(zonesFile);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Matrices loadDemandMatrices(String baseMatricesPath, Zones zones, MatrixFactory matrixFactory) throws IOException, ZoneNotFoundException {
        MatricesParser parser = DemandMatricesParserFactory.createParser(baseMatricesPath, zones, matrixFactory);
        return parser.parse();
    }

    /**
     * Applies correction factors to the demand matrices by parsing each factor matrix file and multiplying the demand matrices with the parsed factors.
     *
     * @param matrices the demand matrices to which correction factors are applied
     * @param factorMatriceFilenames paths to the correction factor matrix files
     * @throws IOException if an I/O error occurs during parsing
     * @throws ZoneNotFoundException if a zone is not found in the lookup
     */
    private void loadAndApplyCorrectionFactors(Matrices matrices, String... factorMatriceFilenames) throws IOException, ZoneNotFoundException {
        for (String filename : factorMatriceFilenames) {
            LOG.info("Applying correction factors from {}", filename);
            CsvFactorMatrixParser parser = new CsvFactorMatrixParser(filename, matrices.getZones(), 1, "\\s+", matrices.getZonesLookup());
            FactorMatrix factorMatrix = parser.parseFactorMatrix();
            matrices.multiplyWith(factorMatrix);
        }
    }
}
