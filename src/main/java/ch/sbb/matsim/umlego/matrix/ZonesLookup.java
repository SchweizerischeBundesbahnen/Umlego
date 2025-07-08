package ch.sbb.matsim.umlego.matrix;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.application.options.CsvOptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ZonesLookup object is used as single source of truth in respect to which zones are considered in Umlego.
 * TODO: this should be used as a Singleton.
 */
public class ZonesLookup {

    /**
     * Column for the market area (cluster) in the CSV file.
     */
    public static final String CLUSTER_COLUMN = "MARKTGEBIETVARELAST";

    private final Map<String, Integer> zonalLookup;

    private final String[] idLookup;

    /**
     * Lookup for zone names to a cluster id (or market area).
     */
    private final Map<String, String> clusterLookup = new HashMap<>();


    /**
     * Constructs a ZonesLookup object by parsing a CSV file containing zone information. Assumes semicolon as separator.
     *
     * @param zonesCsvFileName the path to the CSV file containing zone information
     */
    public ZonesLookup(String zonesCsvFileName) throws IOException {

        zonalLookup = new Object2IntOpenHashMap<>();
        Character delimiter = CsvOptions.detectDelimiter(zonesCsvFileName);

        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setHeader().setSkipHeaderRecord(true).build();
        try (CSVParser parser = new CSVParser(new BufferedReader(new FileReader(zonesCsvFileName)), format)) {

            if (!parser.getHeaderNames().contains("NAME")) {
                throw new IllegalArgumentException("CSV file must contain 'NAME' column.");
            }

            if (!parser.getHeaderNames().contains("NO")) {
                throw new IllegalArgumentException("CSV file must contain 'NO' column.");
            }

            for (CSVRecord r : parser) {

                String name = r.get("NAME");
                int id = Integer.parseInt(r.get("NO"));
                zonalLookup.put(name, id);

                if (r.isMapped(CLUSTER_COLUMN)) {
                    clusterLookup.put(name, r.get(CLUSTER_COLUMN));
                }

            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        int length = zonalLookup.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        this.idLookup = new String[length + 1];
        for (Map.Entry<String, Integer> entry : zonalLookup.entrySet()) {
            this.idLookup[entry.getValue()] = entry.getKey();
        }
    }

    public ZonesLookup(Map<String, Integer> zonalLookup) {
        this.zonalLookup = zonalLookup;

        int length = zonalLookup.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        this.idLookup = new String[length + 1];
        for (Map.Entry<String, Integer> entry : zonalLookup.entrySet()) {
            this.idLookup[entry.getValue()] = entry.getKey();
        }
    }

    /**
     * @param zone the name of the zone whose index is to be retrieved
     * @return the index of the specified zone
     * @throws ZoneNotFoundException if the zone is not found in the lookup
     */
    public Integer getIndex(String zone) throws ZoneNotFoundException {
        Integer result = this.zonalLookup.get(zone);
        if (result == null) {
            throw new ZoneNotFoundException(zone);
        }
        return result;
    }

    /**
     * Retrieves the cluster ID for the specified zone.
     */
    public String getCluster(String zone) {
        if (clusterLookup.isEmpty()) {
            throw new IllegalStateException("Cluster lookup is not initialized. Ensure the CSV file contains '" + CLUSTER_COLUMN + "' column.");
        }

        String result = clusterLookup.get(zone);
        if (result == null) {
            throw new ZoneNotFoundException(zone);
        }

        return result;
    }

    /**
     * Retrieves the zone name corresponding to the specified index.
     *
     * @param index the index of the zone to retrieve
     * @return the name of the zone corresponding to the given index
     * @throws IllegalArgumentException if the index is negative or exceeds the bounds of the lookup array
     */
    public String getZone(int index) {
        if (index < 0 || index >= idLookup.length) {
            throw new IllegalArgumentException("Index out of bounds: " + index);
        }
        return idLookup[index];
    }

    public int getIndex(String zone, Set<String> invalidZoneIds, boolean ignoreExcessZones) throws ZoneNotFoundException {
        try {
            return getIndex(zone);
        } catch (ZoneNotFoundException e) {
            if (!ignoreExcessZones) {
                throw e;
            }
            invalidZoneIds.add(zone);
            return -1;
        }
    }

    public List<String> getAllLookupValues() {
        return this.zonalLookup.keySet().stream().toList();
    }

    public int size() {
        return this.zonalLookup.size();
    }
}
