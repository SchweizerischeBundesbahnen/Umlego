/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2024 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package ch.sbb.matsim.umlego.writers;

import ch.sbb.matsim.routing.pt.raptor.RaptorRoute.RoutePart;
import ch.sbb.matsim.umlego.Umlego.FoundRoute;
import ch.sbb.matsim.umlego.writers.types.volume.Journey;
import ch.sbb.matsim.umlego.writers.types.volume.JourneyItem;
import ch.sbb.matsim.umlego.writers.types.volume.TrainNo;
import com.opencsv.CSVWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class UmlegoBlpWriter implements UmlegoWriter {

    private static final Logger LOG = LogManager.getLogger(UmlegoBlpWriter.class);

    private static final String[] HEADER_ROW = new String[]{
            "TU_CODE",
            "DEPARTURE_ID",
            "TRAIN_NO",
            "INDEX",
            "ARRIVAL",
            "DEPARTURE",
            "TO_STOP_ARRIVAL",
            "FROM_STOP_NO",
            "FROM_STOP_NAME",
            "TO_STOP_NO",
            "TO_STOP_NAME",
            "VOLUME",
            "BOARDING",
            "ALIGHTING",
            "ORIGIN_BOARDING",
            "DESTINATION_ALIGHTING",
    };

    private final String filename;
    private final Map<Id<TransitLine>, Map<Id<TransitRoute>, Map<Id<TransitStopFacility>, Map<Double, TrainNo>>>> trainNos;
    private final Map<TrainNo, Journey> journeyByTrainNo = new HashMap<>();

    /**
     * Writes the aggregated volumes for every train section into a csv file.
     * The columns are:
     * - operatorCode
     * - departureId
     * - fromStopFacilityId
     * - toStopFacilityId
     * - fromStopName
     * - toStopName
     * - arrival
     * - departure
     * - index
     * - volume
     * - boarding
     * - alighting
     * - originBoarding
     * - destinationAlighting
     *
     * @param filename the filename of the outputfile
     * @param schedule the transitSchedule
     */
    public UmlegoBlpWriter(String filename, TransitSchedule schedule) {
        this.filename = filename;

        this.trainNos = new HashMap<>();
        for (TransitLine line : schedule.getTransitLines().values()) {
            HashMap<Id<TransitRoute>, Map<Id<TransitStopFacility>, Map<Double, TrainNo>>> trainNosByRoutes = new HashMap<>();
            for (TransitRoute route : line.getRoutes().values()) {

                HashMap<Id<TransitStopFacility>, Map<Double, TrainNo>> trainNoByStop = new HashMap<>();

                for (TransitRouteStop stop : route.getStops()) {
                    trainNoByStop.put(stop.getStopFacility().getId(), new HashMap<>());
                }

                for (Departure departure : route.getDepartures().values()) {

                    String operatorCode = line.getAttributes().getAttribute("operatorCode").toString();
                    Journey journey = new Journey(new TrainNo(operatorCode, departure.getId()), new ArrayList<>());

                    TransitRouteStop nextStop = null;
                    for (int stopIndex = 0; stopIndex < route.getStops().size() - 1; stopIndex++) {
                        TransitRouteStop curStop = route.getStops().get(stopIndex);
                        nextStop = route.getStops().get(stopIndex + 1);
                        JourneyItem ji = new JourneyItem(curStop, nextStop, departure, stopIndex + 1);
                        journey.items().add(ji);
                        trainNoByStop.get(curStop.getStopFacility().getId())
                            .put(ji.getDeparture(), journey.trainNo());
                    }
                    if (nextStop != null) {
                        // Add last stop
                        JourneyItem ji = new JourneyItem(nextStop, null, departure, journey.items().size() + 1);
                        journey.items().add(ji);
                        trainNoByStop.get(nextStop.getStopFacility().getId())
                            .put(ji.getDeparture(), journey.trainNo());
                    }

                    journeyByTrainNo.put(journey.trainNo(), journey);
                }

                trainNosByRoutes.put(route.getId(), trainNoByStop);
            }
            trainNos.put(line.getId(), trainNosByRoutes);
        }

    }

    @Override
    public void writeRoute(String origZone, String destZone, FoundRoute route) {
        double demand = route.demand;

        for (int i = 0; i<route.stop2stopRoute.routeParts.size(); i++) {
            RoutePart routePart = route.stop2stopRoute.routeParts.get(i);
            if (routePart.line != null && routePart.mode.equals("pt")) {
                routePart.route.getAttributes();

                Map<Id<TransitStopFacility>, Map<Double, TrainNo>> trainNoByStopId = trainNos.get(routePart.line.getId()).get(routePart.route.getId());
                Map<Double, TrainNo> trainNoByDeparture = trainNoByStopId.get(routePart.fromStop.getId());
                TrainNo trainNo = trainNoByDeparture.get(routePart.vehicleDepTime);

                Journey journey = this.journeyByTrainNo.get(trainNo);
                boolean record = false;
                for (JourneyItem item : journey.items()) {
                    if (item.getFromStopFacilityId().equals(routePart.fromStop.getId())
                        && item.getDeparture() == routePart.vehicleDepTime) {
                        record = true;
                        item.addBoarding(demand);
                        if (i == 0) {
                            item.addOriginBoarding(demand);
                        }
                    }

                    if (item.getFromStopFacilityId().equals(routePart.toStop.getId())) {
                        item.addAlighting(demand);
                        if (i == route.stop2stopRoute.routeParts.size() - 1) {
                            item.addDestinationAlighting(demand);
                        }
                        break;
                    }

                    if (record) {
                        item.addDemand(demand);
                    }
                }
            }
        }

    }

    @Override
    public void close() throws Exception {
        LOG.info("Writing BLP matrices...");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(filename));
        CSVWriter writer = new CSVWriter(bufferedWriter, ',', '"', '\\', "\n");
        writer.writeNext(HEADER_ROW);

        for (TrainNo trainNo : this.journeyByTrainNo.keySet()) {
            Journey journey = this.journeyByTrainNo.get(trainNo);
            for (int i = 0; i < journey.items().size(); i++) {
                JourneyItem item = journey.items().get(i);
                JourneyItem nextItem = i + 1 < journey.items().size() ? journey.items().get(i + 1) : null;
                writer.writeNext(new String[]{
                        String.valueOf(journey.trainNo().operatorCode()),
                        String.valueOf(journey.trainNo().departureId()),
                        // Fahrt-ID (differs from Zugnummer in case of Durchbindung - to be implemented)
                        String.valueOf(journey.trainNo().departureId()), // Zugnummer
                        String.valueOf(item.getIndex()), // Item-Index
                        Time.writeTime(item.getArrival()),
                        Time.writeTime(item.getDeparture()),
                        String.valueOf(nextItem == null ? null : Time.writeTime(nextItem.getArrival())),
                        String.valueOf(item.getFromStopFacilityId()),
                        String.valueOf(item.getFromStopName()),
                        String.valueOf(item.getToStopFacilityId()),
                        String.valueOf(item.getToStopName()),
                        Double.toString(item.getVolume()),
                        Double.toString(item.getBoarding()),
                        Double.toString(item.getAlighting()),
                        Double.toString(item.getOriginBoarding()),
                        Double.toString(item.getDestinationAlighting())
                });
            }
        }
        writer.close();
    }
}
