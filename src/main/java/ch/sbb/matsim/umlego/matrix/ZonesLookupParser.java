package ch.sbb.matsim.umlego.matrix;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZonesLookupParser {

    private String fileName;
    private String separator;

    public ZonesLookupParser(String fileName, String separator) {
        this.fileName = fileName;
        this.separator = separator;
    }

    private CSVReader buildCSVReader(BufferedReader bufferedReader) {
        CSVParser csvParser = new CSVParserBuilder().withSeparator(separator.charAt(0)).build();
        return new CSVReaderBuilder(bufferedReader).withCSVParser(csvParser).build();
    }

    public Object2IntMap<String> parseZones() {
        Object2IntMap<String> zonalLookup = new Object2IntOpenHashMap<>();
        zonalLookup.defaultReturnValue(-1);

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
            CSVReader csvReader = buildCSVReader(bufferedReader)) {

            csvReader.skip(1);
            List<String[]> lines = csvReader.readAll();
            for (String[] line : lines) {
                String name = line[0];
                int id = Integer.parseInt(line[1]);
                zonalLookup.put(name, id);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeException(e);
        }
        return zonalLookup;
    }
}
