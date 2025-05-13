package ch.sbb.matsim.umlego.readers.jdbc;

import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.closeConnection;
import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.connect;

import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import ch.sbb.matsim.umlego.readers.jdbc.TimesliceRepository.TimesliceJdbcEntry;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;

public class JdbcDemandMatrixParser extends AbstractJdbcMatrixParser {

    private final String simbaRunId;
    private final String saison;

    public JdbcDemandMatrixParser(String simbaRunId, String saison, ZonesLookup zonesLookup, double defaultValue) {
        super(zonesLookup, defaultValue);
        this.simbaRunId = simbaRunId;
        this.saison = saison;
    }

    /**
     * Parses multiple Database entries to create an instance of DemandMatrices.
     *
     * @return the DemandMatrices generated from the parsed JDBC entries
     * @throws IOException if an error occurs while reading the files
     * @throws ZoneNotFoundException if a zone is not found during parsing
     */
    @Override
    public DemandMatrices parse() throws ZoneNotFoundException {
        TimesliceRepository timesliceRepository = new TimesliceRepository();

        Connection connection = connect();
        List<TimesliceJdbcEntry> resultSetMatrices = timesliceRepository.readMatrices(connection, simbaRunId, saison);
        closeConnection(connection);

        if (resultSetMatrices.isEmpty()) {
            throw new RuntimeException("No timeslices for Simba-Run: " + simbaRunId);
        }
        return jdbcEntriesToDemandMatrices(resultSetMatrices);
    }
}
