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
import ch.sbb.matsim.umlego.writers.types.skim.ODPair;
import ch.sbb.matsim.umlego.writers.types.skim.SkimCalculator;
import ch.sbb.matsim.umlego.writers.types.skim.SkimDemand;
import ch.sbb.matsim.umlego.writers.types.skim.SkimJourneyTime;
import ch.sbb.matsim.umlego.writers.types.skim.SkimNumberOfRoutes;
import ch.sbb.matsim.umlego.writers.types.skim.SkimType;
import ch.sbb.matsim.umlego.writers.types.skim.SkimWeightedAdaptationTime;
import ch.sbb.matsim.umlego.writers.types.skim.SkimWeightedJourneyTime;
import ch.sbb.matsim.umlego.writers.types.skim.SkimWeightedTransfers;
import com.opencsv.CSVWriter;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UmlegoSkimWriter implements UmlegoWriterInterface {

    private static final Logger LOG = LogManager.getLogger(UmlegoSkimWriter.class);

    Set<SkimCalculator> skimCalculators;
    Map<ODPair, Map<SkimType, Double>> skims;
    String filename;

    /**
     * Writes skim matrices to a CSV file. For each origin and destination zone the following Skims are calculated:
     * <ul>
     *     <li>Demand</li>
     *     <li>Journey time</li>
     *     <li>Number of routes</li>
     *     <li>Weighted journey time</li>
     *     <li>Weighted transfers</li>
     *     <li>Weighted adaptation time</li>
     * </ul>
     * <p>
     * The file is written when the {@link #close()} method is called.
     *
     * @param filename the name of the file to write the skim matrices to
     */
    public UmlegoSkimWriter(String filename) {
        this.filename = filename;
        this.skims = new HashMap<>();
        this.skimCalculators = new HashSet<>();

        this.skimCalculators.add(new SkimDemand());
        this.skimCalculators.add(new SkimJourneyTime());
        this.skimCalculators.add(new SkimNumberOfRoutes());
        this.skimCalculators.add(new SkimWeightedJourneyTime());
        this.skimCalculators.add(new SkimWeightedTransfers());
        this.skimCalculators.add(new SkimWeightedAdaptationTime());
    }

    @Override
    public void writeRoute(String origZone, String destZone, FoundRoute route) {
        var key = new ODPair(origZone, destZone);

        var matrices = this.skims.getOrDefault(key, new HashMap<>());
        for (var calculator : this.skimCalculators) {
            var value = matrices.getOrDefault(calculator.getSkimType(), 0.0);
            value = calculator.aggregateRoute(value, destZone, route);
            matrices.put(calculator.getSkimType(), value);
        }
        this.skims.put(key, matrices);
    }

    private String[] createHeaderRow() {
        List<String> headers = new ArrayList<>();
        headers.add("ORIGIN");
        headers.add("DESTINATION");

        for (var skimType : this.skimCalculators) {
            headers.add(skimType.getSkimType().toString());
        }
        return headers.toArray(new String[0]);
    }

    @Override
    public void close() throws Exception {
        LOG.info("Writing skim matrices...");

        var writer = new CSVWriter(new BufferedWriter(new FileWriter(filename)), ',', '"', '\\', "\n");

        writer.writeNext(createHeaderRow());

        for (ODPair odPair : skims.keySet()) {
            var row = new ArrayList<String>();
            row.add(odPair.fromZone());
            row.add(odPair.toZone());
            var matrices = this.skims.get(odPair);
            for (var skimType : this.skimCalculators) {
                row.add(String.format("%.5f", matrices.get(skimType.getSkimType())));
            }
            writer.writeNext(row.toArray(String[]::new));
        }

        writer.close();
        LOG.info("Done with skim matrices.");
    }
}
