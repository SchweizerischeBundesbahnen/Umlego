package ch.sbb.matsim.bewerto.elasticities;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single entry in the elasticity CSV file.
 * Each entry contains parameters for calculating demand elasticity for a specific
 * segment, cluster, and skim type.
 *
 * @param cluster      The cluster number
 * @param segment      The demand segment (DSeg)
 * @param description  Description of the entry
 * @param skimType     The skim type (JRT, NTR, ADT, PM)
 * @param elasticity0  Base elasticity value
 * @param a            Parameter a for elasticity calculation
 * @param b            Parameter b for elasticity calculation
 * @param min          Minimum elasticity value
 * @param max          Maximum elasticity value
 * @param fMin         Minimum factor value
 * @param fMax         Maximum factor value
 * @param kgMax        kg_max value (optional, can be null)
 */
public record ElasticityEntry(
    int cluster,
    String segment,
    String description,
    SkimType skimType,
    double elasticity0,
    double a,
    double b,
    double min,
    double max,
    double fMin,
    double fMax,
    Double kgMax
) {
    private static final Logger LOG = LogManager.getLogger(ElasticityEntry.class);
    private static final char DEFAULT_SEPARATOR = ';';

    /**
     * Reads all elasticity entries from a CSV file.
     *
     * @param filePath Path to the elasticity CSV file
     * @return List of all elasticity entries from the file
     * @throws IOException If there's an error reading the file
     */
    public static List<ElasticityEntry> readAllEntries(String filePath) {
        List<ElasticityEntry> entries = new ArrayList<>();
        
        // Configure CSV parser
        CSVParser parser = new CSVParserBuilder()
                .withSeparator(DEFAULT_SEPARATOR)
                .withIgnoreQuotations(false)
                .build();
        
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(filePath))
                .withCSVParser(parser)
                .build()) {
            
            // Skip header
            String[] header = reader.readNext();
            if (header == null) {
                LOG.warn("Empty elasticity file: {}", filePath);
                return entries;
            }

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 11) {
                    LOG.warn("Skipping malformed line in elasticity file (not enough columns)");
                    continue;
                }
                
                try {
                    int cluster = Integer.parseInt(line[0].trim());
                    String segment = line[1].trim();
                    String description = line[2].trim();
                    SkimType skimType = SkimType.valueOf(line[3].trim());
                    double elasticity0 = Double.parseDouble(line[4].trim());
                    double a = Double.parseDouble(line[5].trim());
                    double b = Double.parseDouble(line[6].trim());
                    double min = Double.parseDouble(line[7].trim());
                    double max = Double.parseDouble(line[8].trim());
                    double fMin = Double.parseDouble(line[9].trim());
                    double fMax = Double.parseDouble(line[10].trim());
                    
                    // kg_max is optional
                    Double kgMax = null;
                    if (line.length > 11 && !line[11].trim().isEmpty()) {
                        kgMax = Double.parseDouble(line[11].trim());
                    }
                    
                    ElasticityEntry entry = new ElasticityEntry(
                        cluster, segment, description, skimType,
                        elasticity0, a, b, min, max, fMin, fMax, kgMax
                    );
                    
                    entries.add(entry);
                    
                } catch (NumberFormatException e) {
                    LOG.warn("Error parsing numeric value in line: {}", String.join(";", line), e);
                }
            }
            
            LOG.info("Loaded {} elasticity entries from file: {}", entries.size(), filePath);
        } catch (CsvValidationException e) {
            LOG.error("CSV validation error in file {}: {}", filePath, e.getMessage());
            throw new RuntimeException("Error validating CSV file", e);
        } catch (IOException e) {
            LOG.error("Error reading elasticity file: {}", filePath, e);
            throw new UncheckedIOException("Error reading elasticity file", e);
        }
        
        return entries;
    }
}
