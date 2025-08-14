package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.AbstractMatrix;
import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.Zones;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;
import io.jhdf.api.Group;
import io.jhdf.api.Node;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OmxMatrixParser implements MatricesParser {

    private static final Logger LOG = LogManager.getLogger(OmxMatrixParser.class);
    private final Zones zones;
    private final String path;
    private final MatrixFactory matrixFactory;

    public OmxMatrixParser(String path, Zones zones, MatrixFactory matrixFactory) {
        this.zones = zones;
        this.path = path;
        this.matrixFactory = matrixFactory;
    }

    /**
     * Parses demand matrices from an OMX file located at the given path. Retrieves matrices from the OMX file, creates DemandMatrix objects, and validates the zone lookup.
     *
     * @return a DemandMatrices object containing the parsed demand matrices
     * @throws ZoneNotFoundException if a zone is not found in the lookup
     */
    @Override
    public Matrices parse() throws ZoneNotFoundException {

        LOG.info("OMX File: {}", path);
        List<AbstractMatrix> matrices = new ArrayList<>();
        Map<String, Integer> indexLookup = new HashMap<>();

        try (HdfFile hdfFile = new HdfFile(Paths.get(path))) {

            Group data = (Group) hdfFile.getChild("data");

            for (Integer no : this.matrixFactory.getNos()) {
                String name = String.valueOf(no);
                Node node = data.getChild(name);

                if (node instanceof Dataset matrix) {

                    Class<?> javaType = matrix.getJavaType();
                    if (javaType != double.class) {
                        throw new RuntimeException("Only double[][] matrices are supported");
                    }

                    int[] dim = matrix.getDimensions();
                    if (dim.length != 2) {
                        throw new RuntimeException("Only 2D matrices are supported");
                    }

                    double[][] d = (double[][]) matrix.getData();

                    AbstractMatrix m = matrixFactory.createMatrix(no, d);
                    if (m != null) {
                        matrices.add(m);
                    }
                }
            }

            Group lookups = (Group) hdfFile.getChild("lookup");
            Map<String, Node> indexes = lookups.getChildren();

            if (indexes.size() != 1) {
                throw new AssertionError("Requires one lookup index");
            }

            Dataset lookup = (Dataset) indexes.values().stream().findFirst().orElseThrow();

            // Validate OMX's lookup with own
            Class<?> t = lookup.getJavaType();
            if (t == long.class) {
                long[] lookupValues = (long[]) lookup.getData();
                for (int index = 0; index < lookupValues.length; index++) {

                    var no = String.valueOf(lookupValues[index]);
                    indexLookup.put(no, index);

                }

                if (lookupValues.length != this.zones.size()) {
                    throw new AssertionError("OMX lookup size does not match zonal lookup size");
                }
            } else if (t == int.class) {
                int[] lookupValues = (int[]) lookup.getData();
                for (int index = 0; index < lookupValues.length; index++) {
                    var no = String.valueOf(lookupValues[index]);
                    indexLookup.put(no, index);

                }

                if (lookupValues.length != this.zones.size()) {
                    throw new AssertionError("OMX lookup size does not match zonal lookup size");
                }

            } else {
                throw new RuntimeException("Unsupported lookup type: " + t.getName());
            }
        }

        var zoneNos = new HashSet<>(this.zones.getAllZoneNos());
        var omxZoneNos = indexLookup.keySet();

        if (!omxZoneNos.containsAll(zoneNos)) {
            throw new ZoneNotFoundException("OMX lookup does not contain all zones. Following Zone Nos are missing: " + zoneNos.removeAll(omxZoneNos));
        }

        if (!zoneNos.containsAll(omxZoneNos)) {
            throw new ZoneNotFoundException("OMX contains additional zones with Nos: " + omxZoneNos.removeAll(zoneNos));
        }

        var zonesLookup = new ZonesLookup(indexLookup);

        return new Matrices(matrices, this.zones, zonesLookup);
    }

}
