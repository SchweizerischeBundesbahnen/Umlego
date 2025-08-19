package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import java.io.IOException;

/**
 * An interface for classes that parse demand matrices from a given path.
 * The classes implementing this interface should be able to parse the demand
 * matrices from the given path and return a {@link Matrices} object.
 * Implementing classes should also throw an {@link IOException} if an error
 * occurs while reading the file and a {@link ZoneNotFoundException} if a zone
 * is not found in the lookup.
 */
public interface MatricesParser {

    /**
     * Parses demand matrices from the given path or simbaRunId and saison.
     *
     * @return a DemandMatrices object containing the parsed demand matrices
     * @throws ZoneNotFoundException if a zone is not found in the lookup
     */
    Matrices parse() throws ZoneNotFoundException;

}
