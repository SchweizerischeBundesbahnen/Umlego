package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.*;
import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;
import io.jhdf.api.Group;
import io.jhdf.api.Node;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Paths;
import java.util.Map;

import static ch.sbb.matsim.umlego.matrix.MatrixUtil.matrixNameToMinutes;

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
        Int2ObjectMap<DemandMatrix> matrices = new Int2ObjectAVLTreeMap<>();

        try (HdfFile hdfFile = new HdfFile(Paths.get(path))) {

            Group data = (Group) hdfFile.getChild("data");

            for (Node node : data) {
                if (node instanceof Dataset matrix) {

                    Class<?> javaType = matrix.getJavaType();
                    if (javaType != double.class) {
                        throw new RuntimeException("Only double[][] matrices are supported");
                    }

                    int[] dim = matrix.getDimensions();
                    if (dim.length != 2) {
                        throw new RuntimeException("Only 2D matrices are supported");
                    }

                    String name = matrix.getName();
                    int startTimeMin = matrixNameToMinutes(name);

                    double[][] d = (double[][]) matrix.getData();

                    matrices.put(startTimeMin, new DemandMatrix(startTimeMin, startTimeMin + MatrixUtil.TIME_SLICE_MIN, d));
                }
            }

            Group lookups = (Group) hdfFile.getChild("lookup");
            Map<String, Node> indexes = lookups.getChildren();

            assert indexes.size() == 1 : "Requires one lookup index";

            Dataset lookup = (Dataset) indexes.values().stream().findFirst().orElseThrow();

            // Validate OMX's lookup with own
            Class<?> t = lookup.getJavaType();
            if (t == long.class) {
                long[] lookupValues = (long[]) lookup.getData();
                for (int i = 0; i < lookupValues.length; i++) {
                    assert i == this.zonalLookup.getIndex(String.valueOf(lookupValues[i]));
                }

            } else if (t == int.class) {
                int[] lookupValues = (int[]) lookup.getData();
                for (int i = 0; i < lookupValues.length; i++) {
                    assert i == this.zonalLookup.getIndex(String.valueOf(lookupValues[i]));
                }
            } else
                throw new RuntimeException("Unsupported lookup type: " + t.getName());
        }

        return new DemandMatrices(matrices.values().stream().toList(), this.zonalLookup);
    }

}
