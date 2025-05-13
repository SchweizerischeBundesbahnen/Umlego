package ch.sbb.matsim.umlego;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicles;

/**
 * The Hafas2UmlegoSchedule converts a set of HAFAS timetable files to MATSim schedule file format. Conversion is delegated to HafasConverter class.
 * <p>
 * Probably based on https://github.com/matsim-org/pt2matsim/blob/master/src/main/java/org/matsim/pt2matsim/run/Hafas2TransitSchedule.java
 */
public final class Hafas2UmlegoSchedule {

    public final static Logger LOG = LogManager.getLogger(Hafas2UmlegoSchedule.class);

    public final static String TSYS_STATS_GROUP = "TSys_Stats";
    private final static Set<String> VEHICLE_TYPES = new HashSet<>(Arrays.asList(

        // TODO: move this list to a properties file
        // "B",     //	Bus   -> nur Bahnersatz, wird anders behandelt. Siehe FPLANReader.
        "AVE",      // Alta Velocidad Espanola
        "CC",       // Zahnradbahn
        "D",        // Schnellzug
        "E",        // Eilzug
        "EC",       // EuroCity
        "EN",       // EuroNight
        "EV",       // Ersatzverkehr
        "FB",       // Frecciabianca
        "FLX",      // FlixTrain
        "FR",       // Frecciarossa
        "IC",       // InterCity
        "ICE",      // InterCityExpress
        "IN",       // InterCityNacht
        "IR",       // InterRegio
        "IRE",      // Interregio-Express
        "NJ",       // nightjet
        "PE",       // PanoramaExpress
        "R",        // Regio
        "RB",       // Regionalbahn
        "RE",       // RegioExpress
        "RJ",       // Railjet
        "RJX",      // Railjet Xpress
        "S",        // S-Bahn
        "SN",       // Nacht-S-Bahn
        "TER",      // Train Express Regional
        "TGV",      // Train à grande vit.
        "WB"        // Westbahn
    ));

    /**
     * Converts all files in <tt>hafasFolder</tt> and writes the output schedule and vehicles to the respective
     * files. Adapts the <tt>zoneConnectionsFile</tt> to the resulting schedule.
     * A pseudo network is generated for the schedule.
     *
     * @param args <br/>
     *             [0] hafasFolder<br/>
     *             [1] zoneConnectionsFile<br/>
     *             [2] outputFolder<br/>
     *             [3] chosenDate for which to build schedule, formatted as yyyy-MM-dd<br/>
     *             [4] (optional) stopsFilterFile<br/>
     */

    /**
     * Adds an attribute to all transit routes in the schedule, grouping them into replacement services (Ersatzverkehre, EV), rail (RAIL), or other (OTHER). The attribute is named TSYS_STATS_GROUP and
     * is used later to group the routes for statistics.
     */
    private void addStatsAttributes(TransitSchedule schedule, Vehicles vehicles) {
        enum VsysGroup {EV, RAIL, OTHER}
        Map<String, VsysGroup> vsysGroupMap = new HashMap<>(Map.of("B", VsysGroup.EV, "EV", VsysGroup.EV));
        VEHICLE_TYPES.forEach(t -> vsysGroupMap.putIfAbsent(t, VsysGroup.RAIL));
        for (TransitLine line : schedule.getTransitLines().values()) {
            for (TransitRoute route : line.getRoutes().values()) {
                String vehicleId = route.getDepartures().values().stream().map(d -> d.getVehicleId().toString()).toList().getFirst();
                String tSysCode = String.valueOf(vehicleId.split("_")[0]);
                VsysGroup group = vsysGroupMap.getOrDefault(tSysCode, VsysGroup.OTHER);
                route.getAttributes().putAttribute(TSYS_STATS_GROUP, group.toString());
            }
        }
    }

    /**
     * Adds walk legs between specific transit stops for the following cases: - Lausanne-Flon <-> Lausanne with a transfer time of 10 minutes ("öV-Zusatz", i.e. to compensate the missing metro) -
     * Altstätten SG <-> Altstätten Stadt with a transfer time of 7 minutes
     *
     * @param schedule the transitSschedule
     */
    private void addSpecialCaseWalkLegs(TransitSchedule schedule) {
        // Lausanne-Flon <-> Lausanne: 10 Min
        Id<TransitStopFacility> lausanneFlon = Id.create("8501181", TransitStopFacility.class);
        Id<TransitStopFacility> lausanne = Id.create("8501120", TransitStopFacility.class);
        schedule.getMinimalTransferTimes().set(lausanneFlon, lausanne, 10 * 60.0);
        schedule.getMinimalTransferTimes().set(lausanne, lausanneFlon, 10 * 60.0);
        // Altsätten SG <-> Altstätten Stadt: 7 Min
        Id<TransitStopFacility> altstaettenSG = Id.create("8506319", TransitStopFacility.class);
        Id<TransitStopFacility> altstaettenStadt = Id.create("8506379", TransitStopFacility.class);
        schedule.getMinimalTransferTimes().set(altstaettenSG, altstaettenStadt, 7 * 60.0);
        schedule.getMinimalTransferTimes().set(altstaettenStadt, altstaettenSG, 7 * 60.0);
    }

}


