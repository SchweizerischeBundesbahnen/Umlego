package ch.sbb.matsim.umlego;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.MinimalTransferTimes;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;

public final class ZoneConnections {

    private static final Logger LOG = LogManager.getLogger(ZoneConnections.class);

    private static final String ZONE = "zone";
    private static final String STOP_POINT = "stop_point";
    private static final String WALK_TIME = "walk_time";

    private ZoneConnections() {}

    public record ConnectedStop(
            String zone,
            double walkTime,
            TransitStopFacility stopFacility) {
    }


    /**
     * Reads zone connections from a CSV file and populates a table with zone IDs, stop facility IDs
     * and walk time.
     * If a stop facility cannot be found in the schedule, a warning is logged.
     * If multiple connections between a stop and a zone are found, they are skipped.
     *
     * @param bufferedReader reader for the CSV file containing zone connections.
     * @param schedule the transit schedule containing stop facilities.
     * @return a table mapping zone IDs and stop facility IDs to connected stop information.
     */
    public static Table<String, Id<TransitStopFacility>, ConnectedStop> readZoneConnections(BufferedReader bufferedReader, TransitSchedule schedule) {
        Table<String, Id<TransitStopFacility>, ConnectedStop> connectionsPerZoneStopPair = HashBasedTable.create();

        Map<Id<TransitStopFacility>, TransitStopFacility> stops = schedule.getFacilities();

        List<String[]> lines = parse(bufferedReader);
        // TODO: remove this shift once we get rid of length in input files (at latest in 2026)
        int shiftOneIfIncludingDeprecatedLengthCol = lines.getFirst().length == 4 ? 1 : 0;
        if (shiftOneIfIncludingDeprecatedLengthCol == 1) {
            LOG.warn("The input connections file includes the deprecated 'length' column. Please update your input file by removing the 'length' column.");
        }
        for (String[] line : lines) {
            String zoneId = line[0];
            double walkTime = Double.parseDouble(line[1 + shiftOneIfIncludingDeprecatedLengthCol]);
            String stopPoint = line[2 + shiftOneIfIncludingDeprecatedLengthCol];
            Id<TransitStopFacility> stopId = Id.create(stopPoint, TransitStopFacility.class);
            TransitStopFacility stopFacility = stops.get(stopId);
            if (stopFacility == null) {
                LOG.warn("stop {} referenced by zone {} cannot be found.", stopPoint, zoneId);
            } else {
                if (connectionsPerZoneStopPair.contains(zoneId, stopFacility.getId())) {
                    LOG.warn("Found multiple connections between stop {} and zone {}. Skipping.", stopPoint, zoneId);
                }
                ConnectedStop connectedStop = new ConnectedStop(zoneId, walkTime, stopFacility);
                connectionsPerZoneStopPair.put(zoneId, stopFacility.getId(), connectedStop);
            }
        }
        return connectionsPerZoneStopPair;
    }

    public static List<String[]> parse(Reader reader) {
        try (CSVReader csvReader = new CSVReader(reader)) {
            csvReader.skip(1);
            return csvReader.readAll();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (CsvException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method takes a table of zone to stop facility connections and fills out all unconnected stops that are within walking distance of a connected stop.
     * For each connected stop h, it copies all connections of h to all stops h_f that are within walking distance of h (if h_f is not already connected).
     * The walk time to h_f is added to the connection time of h.
     * @param connectionsPerZoneStopPair the table of zone to stop facility connections.
     * @param schedule the transit schedule.
     * @return a new table that includes the new connections
     */
    public static Table<String, Id<TransitStopFacility>, ConnectedStop> fillConnectionsWithinWalkingDistance(Table<String, Id<TransitStopFacility>, ConnectedStop> connectionsPerZoneStopPair, TransitSchedule schedule) {
        // Create additional connections to all stops within walking distance and populate final map
        Table<String, Id<TransitStopFacility>, ConnectedStop> newConnectionsPerZoneStopPair = HashBasedTable.create();
        for (Cell<String, Id<TransitStopFacility>, ConnectedStop> zoneStopPair : connectionsPerZoneStopPair.cellSet()) {
            String zoneId = zoneStopPair.getRowKey();
            Id<TransitStopFacility> stopId = zoneStopPair.getColumnKey();
            ConnectedStop connectedStop = zoneStopPair.getValue();
            for (Id<TransitStopFacility> toStopId : getTransferCandidates(schedule.getMinimalTransferTimes(), stopId)) {
                if (connectionsPerZoneStopPair.rowMap().get(zoneId).get(toStopId) == null) {  // there are no Connections already from current zone to target stop
                    double walkingTime = schedule.getMinimalTransferTimes().get(stopId, toStopId);
                    if (!connectedStop.stopFacility.getId().equals(toStopId)) {
                        ConnectedStop newConnectedStop = new ConnectedStop(zoneId, connectedStop.walkTime + walkingTime, schedule.getFacilities().get(toStopId));
                        newConnectionsPerZoneStopPair.put(zoneId, toStopId, newConnectedStop);
                    }
                }
            }
        }
        Table<String, Id<TransitStopFacility>, ConnectedStop> finalConnectionsPerZoneStopPair = HashBasedTable.create();
        finalConnectionsPerZoneStopPair.putAll(connectionsPerZoneStopPair);
        finalConnectionsPerZoneStopPair.putAll(newConnectionsPerZoneStopPair);
        return finalConnectionsPerZoneStopPair;
    }

    public static void writeConnections(String filename, Table<String, Id<TransitStopFacility>, ConnectedStop> connectionsPerZone) throws IOException {
        // TODO: the CSV separator should probably be set once and globally
        CSVWriter writer = new CSVWriter(IOUtils.getBufferedWriter(filename), ',', '"', '\\', "\n");
        writer.writeNext(new String[]{ZONE, WALK_TIME, STOP_POINT});

        for (Cell<String, Id<TransitStopFacility>, ConnectedStop> zoneConnections : connectionsPerZone.cellSet()) {
            ConnectedStop connection = zoneConnections.getValue();
            assert zoneConnections.getColumnKey().equals(connection.stopFacility.getId());
            assert zoneConnections.getRowKey().equals(connection.zone());
            writer.writeNext(new String[]{
                    zoneConnections.getRowKey(),
                    Double.toString(connection.walkTime),
                    String.valueOf(connection.stopFacility.getId())
            }, false);
        }
        writer.close();
    }

    public static Set<Id<TransitStopFacility>> getTransferCandidates(MinimalTransferTimes mtt, Id<TransitStopFacility> fromStop) {
        Set<Id<TransitStopFacility>> result = new HashSet<>();
        MinimalTransferTimes.MinimalTransferTimesIterator it = mtt.iterator();

        while (it.hasNext()) {
            it.next();
            if (it.getFromStopId().equals(fromStop)) {
                result.add(it.getToStopId());
            }
        }
        return result;
    }

}
