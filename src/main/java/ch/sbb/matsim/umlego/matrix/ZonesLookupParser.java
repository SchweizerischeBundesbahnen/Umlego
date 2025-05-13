package ch.sbb.matsim.umlego.matrix;

import ch.sbb.matsim.umlego.config.hadoop.FileSystemUtil;
import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import java.io.FileInputStream;
import java.io.FileReader;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.eclipse.jetty.io.RuntimeIOException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    public Map<String, Integer> parseZones() {
        Map<String, Integer> zonalLookup = new HashMap<>();
        try (FileSystem fs = FileSystemUtil.getFileSystem();
             BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
             CSVReader csvReader = buildCSVReader(bufferedReader)) {

            csvReader.skip(1);
            List<String[]> lines = csvReader.readAll();
            for (String[] line : lines) {
                String name = line[0];
                int id = Integer.parseInt(line[1]);
                zonalLookup.put(name, id);
            }
        } catch (IOException | CsvException e) {
            throw new RuntimeIOException(e);
        }
        return zonalLookup;
    }
}
