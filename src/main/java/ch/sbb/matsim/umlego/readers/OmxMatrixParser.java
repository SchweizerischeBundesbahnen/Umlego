package ch.sbb.matsim.umlego.readers;

import static ch.sbb.matsim.umlego.matrix.MatrixUtil.matrixNameToMinutes;

import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.DemandMatrix;
import ch.sbb.matsim.umlego.matrix.MatrixUtil;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.Zones;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import io.jhdf.HdfFile;
import io.jhdf.api.Dataset;
import io.jhdf.api.Group;
import io.jhdf.api.Node;
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class OmxMatrixParser implements DemandMatricesParser {

    private static final Logger LOG = LogManager.getLogger(OmxMatrixParser.class);
    private final Zones zones;
    private final String path;

    public OmxMatrixParser(String path, Zones zones) {
        this.zones = zones;
        this.path = path;
    }

    /**
     * Parses demand matrices from an OMX file located at the given path. Retrieves matrices from the OMX file, creates DemandMatrix objects, and validates the zone lookup.
     *
     * @return a DemandMatrices object containing the parsed demand matrices
     * @throws ZoneNotFoundException if a zone is not found in the lookup
     */
    @Override
    public DemandMatrices parse() throws ZoneNotFoundException {
        LOG.info("OMX File: {}", path);
        Int2ObjectMap<DemandMatrix> matrices = new Int2ObjectAVLTreeMap<>();
        Map<String, Integer> indexLookup = new HashMap<>();

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

        return new DemandMatrices(matrices.values().stream().toList(), this.zones, zonesLookup);
    }

}
