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

package ch.sbb.matsim.umlego.skims;

import ch.sbb.matsim.umlego.FoundRoute;

import ch.sbb.matsim.umlego.UmlegoWorkResult;
import java.util.List;
import java.util.Map;

/**
 * Utility class for calculating skims values.
 */
public final class UmlegoSkimCalculator {

    public final static UmlegoSkimCalculator INSTANCE = new UmlegoSkimCalculator();

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
     * Calculates skims for the given work result. The result is stored in {@link UmlegoWorkResult#skims()}.
     *
     * @param result The work result containing the routes per destination zone.
     * @param target The target map for storing the skim values.
     */
    public void calculateSkims(UmlegoWorkResult result,  Map<String, double[]> target) {

        target.clear();
        for (Map.Entry<String, List<FoundRoute>> e : result.routesPerDestinationZone().entrySet()) {
            String destZone = e.getKey();

            double[] matrices = new double[this.calculators.size()];
            target.put(destZone, matrices);

            for (int i = 0; i < this.calculators.size(); i++) {
                SkimCalculator calculator = this.calculators.get(i);

                for (FoundRoute route : e.getValue()) {
                    matrices[i] = calculator.aggregateRoute(matrices[i], destZone, route);
                }

                if (calculator.isNormalizedByDemand()) {
                    matrices[i] = matrices[i] / matrices[0]; // Demand is at index 0
                }
            }

        }
    }
}
