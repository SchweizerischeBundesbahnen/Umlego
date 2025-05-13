package ch.sbb.matsim.umlego.readers.jdbc;

import ch.sbb.matsim.umlego.matrix.DemandMatrices;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import ch.sbb.matsim.umlego.readers.DemandMatricesParser;
import ch.sbb.matsim.umlego.readers.jdbc.TimesliceRepository.TimesliceJdbcEntry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractJdbcMatrixParser implements DemandMatricesParser {

    private static final Logger LOG = LogManager.getLogger(AbstractJdbcMatrixParser.class);

    private final ZonesLookup zonesLookup;
    private final double defaultValue;

    protected AbstractJdbcMatrixParser(ZonesLookup zonesLookup, double defaultValue) {
        this.zonesLookup = zonesLookup;
        this.defaultValue = defaultValue;
    }

    private ZonesLookup getZonesLookup() {
        return zonesLookup;
    }

    private double getDefaultValue() {
        return this.defaultValue;
    }


    protected DemandMatrices jdbcEntriesToDemandMatrices(List<TimesliceJdbcEntry> timesliceJdbcEntries) throws ZoneNotFoundException {
        DemandMatrices demandMatrices = DemandMatrices.getInstance();
        demandMatrices.setZonesLookup(getZonesLookup());

        // Group TimesliceJdbcEntry by matrixIndex
        Map<Integer, List<TimesliceJdbcEntry>> groupedEntries = timesliceJdbcEntries.stream()
            .collect(Collectors.groupingBy(TimesliceJdbcEntry::matrixIndex));

        for (Map.Entry<Integer, List<TimesliceJdbcEntry>> entry : groupedEntries.entrySet()) {
            int index = entry.getKey();
            List<TimesliceJdbcEntry> entries = entry.getValue();
            demandMatrices.addMatrix(index, entries, getDefaultValue());
        }

        return demandMatrices;
    }
}
