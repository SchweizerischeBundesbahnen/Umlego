package ch.sbb.matsim.umlego.matrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.matsim.application.options.CsvOptions;

/**
 * The ZonesLookup object is used as single source of truth in respect to which zones are considered in Umlego.
 * TODO: this should be used as a Singleton.
 */
public class Zones {

    /**
     * Column for the market area (cluster) in the CSV file.
     */
    public static final String CLUSTER_COLUMN = "MARKTGEBIETVARELAST";

    private final Map<String, Zone> zoneByNo;

    /**
     * Constructs a Zones object by parsing a CSV file containing zone information.
     *
     * @param zonesCsvFileName the path to the CSV file containing zone information
     */
    public Zones(String zonesCsvFileName) throws IOException {

        List<Zone> data = new ArrayList<>();
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
                String no = r.get("NO");

                String cluster = null;
                if (r.isMapped(CLUSTER_COLUMN)) {
                    cluster = r.get(CLUSTER_COLUMN);
                }

                var zone = new Zone(no, name, cluster);
                data.add(zone);

            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        this.zoneByNo = data.stream().collect(Collectors.toMap(Zone::getNo, z -> z));

    }

    public Zone getZone(String no) {
        if (!zoneByNo.containsKey(no)) {
            throw new ZoneNotFoundException(no);
        }
        return zoneByNo.get(no);
    }

    public Zones(List<Zone> zones) {
        this.zoneByNo = zones.stream().collect(Collectors.toMap(Zone::getNo, z -> z));

    }

    /**
     * Retrieves the cluster ID for the specified zone.
     */
    public String getCluster(String zone) {
        if (zoneByNo.isEmpty()) {
            throw new IllegalStateException("Cluster lookup is not initialized. Ensure the CSV file contains '" + CLUSTER_COLUMN + "' column.");
        }

        Zone result = zoneByNo.get(zone);
        if (result == null) {
            throw new ZoneNotFoundException(zone);
        }

        return result.getElasticityCluster();
    }

    public List<String> getAllZoneNos() {
        return this.zoneByNo.keySet().stream().toList();
    }

    public int size() {
        return this.zoneByNo.size();
    }

    public ZonesLookup createDefaultZonesLookup() {
        Map<String, Integer> indexByNo = new HashMap<>();
        List<String> zoneNos = this.getAllZoneNos().stream().sorted().toList();
        for (int i = 0; i < zoneNos.size(); i++) {
            indexByNo.put(zoneNos.get(i), i);
        }

        return new ZonesLookup(indexByNo);
    }
}
