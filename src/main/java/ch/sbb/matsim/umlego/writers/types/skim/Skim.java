package ch.sbb.matsim.umlego.writers.types.skim;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The Skim class represents a collection of skimming matrices, which store aggregated travel-related data for
 * origin-destination pairs. Each origin-destination pair is associated with a map of {@link SkimColumn} to Double
 * values.
 */
public class Skim {

    Map<ODPair, Map<SkimColumn, Double>> valueMap;

    /**
     * Default constructor for the {@link Skim} class. Initializes an empty collection of skimming matrices.
     */
    public Skim() {
        this.valueMap = new HashMap<>();
    }

    /**
     * Constructs a  nlink Skim} object with pre-populated values.
     *
     * @param values A map of {@link ODPair} to maps of {@link SkimColumn} and Double values.
     */
    public Skim(Map<ODPair, Map<SkimColumn, Double>> values) {
        this.valueMap = new HashMap<>(values);
    }

    /**
     * Retrieves the skimming matrix for a given origin-destination pair. If the pair does not exist, returns the
     * provided default value.
     *
     * @param key The {@link ODPair} representing the origin-destination pair.
     * @param defaultValue A default map of {@link SkimColumn} to Double to return if the pair is not present.
     * @return The skimming matrix for the given pair, or the default value if not found.
     */
    public Map<SkimColumn, Double> getOrDefault(ODPair key, Map<SkimColumn, Double> defaultValue) {
        return valueMap.getOrDefault(key, defaultValue);
    }

    /**
     * Adds or updates the skimming matrix for a given origin-destination pair.
     *
     * @param key The {@link ODPair} representing the origin-destination pair.
     * @param value A map of {@link SkimColumn} to Double representing the skimming matrix.
     */
    public void put(ODPair key, Map<SkimColumn, Double> value) {
        this.valueMap.put(key, value);
    }

    /**
     * Retrieves all the origin-destination pairs present in the skim.
     *
     * @return A {@link Set} of {@link ODPair} objects.
     */
    public Set<ODPair> keySet() {
        return valueMap.keySet();
    }

    /**
     * Retrieves the skimming matrix for a specific origin-destination pair.
     *
     * @param odPair The {@link ODPair} representing the origin-destination pair.
     * @return A map of {@link SkimColumn} to Double values, or null if the pair is not found.
     */
    public Map<SkimColumn, Double> get(ODPair odPair) {
        return valueMap.get(odPair);
    }
}
