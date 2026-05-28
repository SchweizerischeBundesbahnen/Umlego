package ch.sbb.matsim.umlego.readers;

import ch.sbb.matsim.umlego.matrix.Zones;

import java.io.IOException;

public interface MatricesParserFactory {
    MatricesParser createParser(String filePath, Zones zones, MatrixFactory matrixFactory) throws IOException;
}
