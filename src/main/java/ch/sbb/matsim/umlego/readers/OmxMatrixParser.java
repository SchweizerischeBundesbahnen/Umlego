package ch.sbb.matsim.umlego.readers;

import static ch.sbb.matsim.umlego.matrix.MatrixUtil.matrixNameToMinutes;

import ch.sbb.matsim.umlego.matrix.*;

import java.util.ArrayList;
import java.util.List;
import omx.OmxFile;
import omx.OmxLookup;
import omx.OmxMatrix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OmxMatrixParser implements DemandMatricesParser {

    private static final Logger LOG = LogManager.getLogger(OmxMatrixParser.class);
    private final ZonesLookup zonalLookup;
    private final String path;

    public OmxMatrixParser(String path, ZonesLookup zonesLookup) {
        this.zonalLookup = zonesLookup;
        this.path = path;
    }

    /**
     * Parses demand matrices from an OMX file located at the given path.
     * Retrieves matrices from the OMX file, creates DemandMatrix objects, and validates the zone lookup.
     *
     * @return a DemandMatrices object containing the parsed demand matrices
     * @throws ZoneNotFoundException if a zone is not found in the lookup
     */
    @Override
    public DemandMatrices parse() throws ZoneNotFoundException {
        LOG.info("OMX File: {}", path);
        List<DemandMatrix> matrices = new ArrayList<>();
        try (OmxFile omxFile = new OmxFile(path)) {
            omxFile.openReadOnly();
            LOG.info("Found the following matrices: {}", omxFile.getMatrixNames());
            for (String m : omxFile.getMatrixNames()) {
                OmxMatrix.OmxDoubleMatrix omxMatrix = (OmxMatrix.OmxDoubleMatrix) omxFile.getMatrix(m);
                int startTimeMin = matrixNameToMinutes(m);
                matrices.add(new DemandMatrix(startTimeMin, startTimeMin + MatrixUtil.TIME_SLICE_MIN, omxMatrix.getData()));
            }
            // Validate OMX's lookup with own
            assert omxFile.getLookupNames().size() == 1;
            String lookupName = omxFile.getLookupNames().iterator().next();
            OmxLookup.OmxIntLookup lookup = (OmxLookup.OmxIntLookup) omxFile.getLookup(lookupName);
            int[] lookupValues = lookup.getLookup();
            for (int i = 0; i < lookupValues.length; i++) {
                assert i == this.zonalLookup.getIndex(String.valueOf(lookupValues[i]));
            }
        }
        return new DemandMatrices(matrices, this.zonalLookup);
    }

}
