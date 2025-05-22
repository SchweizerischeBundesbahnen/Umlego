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

import ch.sbb.matsim.umlego.Umlego.FoundRoute;
import com.opencsv.CSVWriter;
import org.matsim.core.utils.misc.Time;

import java.io.IOException;
import java.util.Locale;

public class UmlegoCsvWriter implements UmlegoWriterInterface {

    private static final String[] HEADER_ROW = new String[]{
        "ORIGZONENO",
        "DESTZONENO",
        "ORIGNAME",
        "DESTNAME",
        "ACCESS_TIME",
        "EGRESS_TIME",
        "DEPTIME",
        "ARRTIME",
        "TRAVTIME",
        "NUMTRANSFERS",
        "DISTANZ",
        "DEMAND",
        "DETAILS"
    };

    private final boolean writeDetails;
    private final CSVWriter writer;

    public UmlegoCsvWriter(String filename, boolean writeDetails) throws IOException {
        this.writeDetails = writeDetails;
        this.writer = new CSVWriter(UmlegoWriter.newBufferedWriter(filename), ',', '"', '\\', "\n");
        this.writer.writeNext(HEADER_ROW);
    }

    @Override
    public void writeRoute(String origZone, String destZone, FoundRoute route) {
        this.writer.writeNext(new String[]{
            origZone,
            destZone,
            route.originStop.getName(),
            route.destinationStop.getName(),
            Time.writeTime(route.originConnectedStop.walkTime()),
            Time.writeTime(route.destinationConnectedStop.walkTime()),
            Time.writeTime(route.depTime),
            Time.writeTime(route.arrTime),
            Time.writeTime(route.travelTimeWithoutAccess),
            Integer.toString(route.transfers),
            String.format(Locale.US,"%.2f", route.distance / 1000.0),
            String.format(Locale.US,"%.5f", route.demand.getDouble(destZone)),
            this.writeDetails ? route.getRouteAsString() : ""
        });
    }

    @Override
    public void close() throws Exception {
        this.writer.close();
    }
}
