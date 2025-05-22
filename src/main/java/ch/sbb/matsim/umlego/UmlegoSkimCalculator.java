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

import java.util.*;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

public final class UmlegoSkimCalculator implements UmlegoListener {

    private final Set<SkimCalculator> calculators;
    private final Map<ODPair, Object2DoubleMap<SkimType>> skims;

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
        this.calculators = new LinkedHashSet<>();

        this.calculators.add(new SkimDemand());
        this.calculators.add(new SkimJourneyTime());
        this.calculators.add(new SkimNumberOfRoutes());
        this.calculators.add(new SkimWeightedJourneyTime());
        this.calculators.add(new SkimWeightedTransfers());
        this.calculators.add(new SkimWeightedAdaptationTime());
    }

    public Set<SkimCalculator> getCalculators() {
        return calculators;
    }

    public Map<ODPair, Object2DoubleMap<SkimType>> getSkims() {
        return skims;
    }

    @Override
    public void processRoute(String origZone, String destZone, FoundRoute route) {
        var key = new ODPair(origZone, destZone);

        var matrices = this.skims.getOrDefault(key, new Object2DoubleOpenHashMap<>());
        for (SkimCalculator calculator : this.calculators) {
            double value = matrices.getOrDefault(calculator.getSkimType(), 0.0);
            value = calculator.aggregateRoute(value, destZone, route);
            matrices.put(calculator.getSkimType(), value);
        }
        this.skims.put(key, matrices);
    }
}
