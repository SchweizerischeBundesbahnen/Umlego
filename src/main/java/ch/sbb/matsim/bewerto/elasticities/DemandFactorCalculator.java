package ch.sbb.matsim.bewerto.elasticities;

import ch.sbb.matsim.umlego.UmlegoSkimCalculator;
import ch.sbb.matsim.umlego.matrix.ZonesLookup;
import ch.sbb.matsim.umlego.writers.types.skim.ODPair;
import com.opencsv.CSVWriter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;

/**
 * The {@code DemandFactorCalculator} class is responsible for calculating demand factors.
 * <p>
 * The calculator uses two {@link UmlegoSkimCalculator} instances to compute the demand factors based on elasticities.
 */
public final class DemandFactorCalculator {

    /**
     * Empty array to represent no data.
     */
    private final static double[] EMPTY = new double[0];

    private final ZonesLookup lookup;
    private final UmlegoSkimCalculator baseSkim;
    private final UmlegoSkimCalculator variantSkim;

    /**
     * Contains the entries for elasticity calculation. Mapped by {@link ElasticityEntry#cluster()} and {@link ElasticityEntry#skimType()}.
     */
    private final Int2ObjectMap<Map<SkimType, ElasticityEntry>> entries = new Int2ObjectOpenHashMap<>();

    /**
     * Cache for calculated factors.
     */
    private final Object2DoubleMap<ODPair> factors = new Object2DoubleOpenHashMap<>();

    /**
     * Separate factors, used for output.
     */
    private final Map<ODPair, double[]> indvFactors = new HashMap<>();

    /**
     * Constructs a {@code DemandFactorCalculator} used to calculate demand factors
     * by comparing base and variant skim matrices for given origin-destination zones.
     *
     * @param file        the file path containing demand elasticity parameters
     * @param segment     the specific demand segment to be applied
     * @param lookup      zone lookup object
     * @param baseSkim    the base skim calculator containing metrics for the base scenario
     * @param variantSkim the variant skim calculator containing metrics for the variant scenario
     */
    public DemandFactorCalculator(String file, String segment, ZonesLookup lookup,
                                  UmlegoSkimCalculator baseSkim, UmlegoSkimCalculator variantSkim) {
        this.lookup = lookup;
        this.baseSkim = baseSkim;
        this.variantSkim = variantSkim;

        ElasticityEntry.readAllEntries(file).stream()
                .filter(e -> Objects.equals(e.segment(), segment))
                .forEach(entry -> {
                    entries.computeIfAbsent(entry.cluster(), k -> new EnumMap<>(SkimType.class))
                            .put(entry.skimType(), entry);
                });

        if (entries.isEmpty()) {
            throw new IllegalArgumentException("No elasticity entries found for segment " + segment);
        }
    }

    /**
     * Retrieves the demand factor for a given origin-destination pair.
     */
    public double calculateFactor(String origZone, String destZone) {
        return factors.computeIfAbsent(new ODPair(origZone, destZone), this::computeFactors);
    }

    /**
     * Writes calculated demand factors to a specified file.
     *
     * @param file the path of the file where the demand factors will be written
     */
    public void writeFactors(String file) {
        try (FileWriter fileWriter = new FileWriter(file);
             CSVWriter writer = new CSVWriter(fileWriter, ';', CSVWriter.NO_QUOTE_CHARACTER, 
                                              CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END)) {
            // Write header row
            writer.writeNext(new String[]{"From", "To", "F_JRT", "F_ADT", "F_NTR", "TotalFactor"});
            
            // Write data rows
            for (Map.Entry<ODPair, double[]> e : indvFactors.entrySet()) {
                double[] factorValues = e.getValue();
                
                // Calculate total factor as the product of individual factors
                double totalFactor = factorValues[0] * factorValues[1] * factorValues[2];
                
                String[] row = {
                    e.getKey().fromZone(),
                    e.getKey().toZone(),
                    String.format(Locale.US, "%.6f", factorValues[0]),
                    String.format(Locale.US, "%.6f", factorValues[1]),
                    String.format(Locale.US, "%.6f", factorValues[2]),
                    String.format(Locale.US, "%.6f", totalFactor)
                };
                
                writer.writeNext(row);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Error writing elasticity factors to file: " + file, e);
        }
    }

    /**
     * Computation of the demand factor.
     */
    private double computeFactors(ODPair od) {

        double[] variantValues = variantSkim.getSkims().getOrDefault(od, EMPTY);
        if (variantValues == EMPTY) {
            return 1;
        }

        double[] baseValues = baseSkim.getSkims().getOrDefault(od, EMPTY);
        if (baseValues == EMPTY) {
            return 1;
        }

        // TODO: correct cluster calculation
        String cluster = lookup.getCluster(od.fromZone());

        // TODO: OG_A PZ ?

        double ax = Math.min(baseValues[UmlegoSkimCalculator.ADT_IDX], Double.MAX_VALUE) / 15;
        double bx = baseValues[UmlegoSkimCalculator.JRT_IDX] / 45;

        double eJRT = computeElasticity(entries.get(cluster).get(SkimType.JRT), ax, bx);
        double FJRT = computeFactor(variantValues, baseValues, UmlegoSkimCalculator.JRT_IDX, eJRT);

        double eADT = computeElasticity(entries.get(cluster).get(SkimType.ADT), ax, bx);
        double FADT = computeFactor(variantValues, baseValues, UmlegoSkimCalculator.ADT_IDX, eADT);

        double eNTR = computeElasticity(entries.get(cluster).get(SkimType.NTR), ax, bx);
        double FNTR = computeFactor(variantValues, baseValues, UmlegoSkimCalculator.NTR_IDX, eNTR);
        
        indvFactors.put(od, new double[]{FJRT, FADT, FNTR});
        
        return FJRT * FADT * FNTR;
    }

    private double computeElasticity(ElasticityEntry e, double ax, double bx) {
        return Math.max(e.min(), Math.min(e.max(), e.elasticity0() + e.a() * ax + e.b() * bx));
    }

    private double computeFactor(double[] variantValues, double[] baseValues, int idx, double e) {
        if (baseValues[idx] == 0) {
            // If the base value is zero, we cannot compute a factor, return 1
            return 1;
        }

        return Math.pow(variantValues[idx] / baseValues[idx], e);
    }

}
