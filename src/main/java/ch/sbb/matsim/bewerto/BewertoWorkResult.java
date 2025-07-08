package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.umlego.WorkResult;
import java.util.Map;

/**
 * Result of a Bewerto work item. This is generated once per variant.
 *
 * @param originZone the origin zone
 * @param factors a map of computed demand factors for each destination zone
 */
public record BewertoWorkResult(
    String originZone,
    Map<String, double[]> factors
) implements WorkResult {

}
