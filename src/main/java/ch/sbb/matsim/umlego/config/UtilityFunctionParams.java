package ch.sbb.matsim.umlego.config;

import ch.sbb.matsim.umlego.RouteUtilityCalculator;
import ch.sbb.matsim.umlego.RouteUtilityCalculators;

import java.util.Map;

/**
 * Utility function parameters for the route selection.
 * @param params
 */
public record UtilityFunctionParams(Type type, Map<String, Double> params) {

    /**
     * Reads the utility function parameters and creates a RouteUtilityCalculator.
     */
    public RouteUtilityCalculator createUtilityCalculator() {

        return switch (type) {
            case Type.boxcox -> RouteUtilityCalculators.boxcox(params.get("beta"), params.get("tau"));
            case Type.lohse -> RouteUtilityCalculators.lohse(params.get("beta"));
        };
    }

    public enum Type {
        boxcox,
        lohse
    }
}
