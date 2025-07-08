package ch.sbb.matsim.umlego.matrix;

import java.util.Map;

public class ZonesLookup {

    Map<String, Integer> indexByNo;

    public ZonesLookup(Map<String, Integer> indexByNo) {
        this.indexByNo = indexByNo;
    }

    public int getIndex(String zoneNo) {
        Integer index = this.indexByNo.get(zoneNo);
        if (index == null) {
            throw new ZoneNotFoundException("Zone with no " + zoneNo + " not found in lookup.");
        }
        return index;
    }

}
