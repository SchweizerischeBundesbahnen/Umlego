package ch.sbb.matsim.umlego.matrix;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The ZonesLookup object is used as single source of truth in respect to which zones are considered in Umlego.
 * TODO: this should be used as a Singleton.
 */
public class ZonesLookup {

    private final Map<String, Integer> zonalLookup;

    /**
     * Constructs a ZonesLookup object by parsing a CSV file containing zone information. Assumes semicolon as separator.
     *
     * @param zonesCsvFileName the path to the CSV file containing zone information
     */
    public ZonesLookup(String zonesCsvFileName) {
        this(zonesCsvFileName, ";");
    }

    /**
     * Constructs a ZonesLookup object by parsing a CSV file containing zone information.
     *
     * @param zonesCsvFileName the path to the CSV file containing zone information
     * @param separator the separator used in the CSV file
     */
    private ZonesLookup(String zonesCsvFileName, String separator) {
        ZonesLookupParser parser = new ZonesLookupParser(zonesCsvFileName, separator);
        this.zonalLookup = parser.parseZones();
    }

    public ZonesLookup(Map<String, Integer> zonalLookup) {
        this.zonalLookup = zonalLookup;
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
