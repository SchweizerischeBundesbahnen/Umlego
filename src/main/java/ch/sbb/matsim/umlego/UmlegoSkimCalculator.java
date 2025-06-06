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

package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.writers.types.skim.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UmlegoSkimCalculator implements UmlegoListener {

    /**
     * Index of the journey time in the skim array.
     */
    public static final int JRT_IDX = 3;

    /**
     * Index of the number of transfers in the skim array.
     */
    public static final int NTR_IDX = 4;

    /**
     * Index of the adaptation time in the skim array.
     */
    public static final int ADT_IDX = 5;

    private final List<SkimCalculator> calculators;
    private final Map<ODPair, double[]> skims;

    /**
     * Calculate skim matrices. For each origin and destination zone the following Skims are calculated:
     * <ul>
     *     <li>Demand</li>
     *     <li>Journey time</li>
     *     <li>Number of routes</li>
     *     <li>Weighted journey time</li>
     *     <li>Weighted transfers</li>
     *     <li>Weighted adaptation time</li>
     * </ul>
     * <p>
     */
    public UmlegoSkimCalculator() {
        this.skims = new HashMap<>();

        // Don't change something here without checking the DemandFactorCalculator, it depends on the indices of the calculators
        this.calculators = List.of(
                new SkimDemand(),
                new SkimJourneyTime(),
                new SkimNumberOfRoutes(),
                new SkimWeightedJourneyTime(),
                new SkimWeightedTransfers(),
                new SkimWeightedAdaptationTime()
        );
    }

    /**
     * Retrieves the set of skim calculators used for calculating various skim metrics.
     */
    public List<SkimCalculator> getCalculators() {
        return calculators;
    }

    /**
     * Retrieves the skim matrices for origin-destination zone pairs. Each origin-destination pair is associated with an array of doubles
     * that represent various calculated skim metrics such as demand, journey time, number of routes, weighted journey time, weighted
     * transfers, and weighted adaptation time.
     */
    public Map<ODPair, double[]> getSkims() {
        return skims;
    }

    @Override
    public void processRoute(String origZone, String destZone, FoundRoute route) {
        ODPair key = new ODPair(origZone, destZone);

        double[] matrices = this.skims.computeIfAbsent(key, (k) -> new double[this.calculators.size()]);

        for (int i = 0; i < this.calculators.size(); i++) {
            SkimCalculator calculator = this.calculators.get(i);
            matrices[i] = calculator.aggregateRoute(matrices[i], destZone, route);
        }

        this.skims.put(key, matrices);
    }

    @Override
    public void processODPair(String origZone, String destZone) {
        double[] entry = skims.get(new ODPair(origZone, destZone));

        for (int i = 0; i < this.calculators.size(); i++) {
            SkimCalculator calculator = this.calculators.get(i);
            if (calculator.isNormalizedByDemand()) {
                entry[i] = entry[i] / entry[0]; // Demand is at index 0
            }
        }
    }

}
