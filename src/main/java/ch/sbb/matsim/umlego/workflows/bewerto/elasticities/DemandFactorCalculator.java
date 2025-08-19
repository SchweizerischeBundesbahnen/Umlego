package ch.sbb.matsim.umlego.workflows.bewerto.elasticities;

import ch.sbb.matsim.umlego.matrix.DemandMatrixMultiplier;
import ch.sbb.matsim.umlego.matrix.Matrices;
import ch.sbb.matsim.umlego.matrix.Zones;
import ch.sbb.matsim.umlego.skims.UmlegoSkimCalculator;
import ch.sbb.matsim.umlego.workflows.bewerto.BewertoWorkResult;
import ch.sbb.matsim.umlego.workflows.bewerto.config.ElasticitiesParameters;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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

    private final ElasticitiesParameters params;
    private final Matrices matrices;

    /**
     * Contains the entries for elasticity calculation. Mapped by {@link ElasticityEntry#cluster()} and {@link ElasticityEntry#skimType()}.
     */
    private final Map<String, Map<String, Map<SkimType, ElasticityEntry>>> entriesBySegmentAndCluster = new LinkedHashMap<>();

    /**
     * Constructs a {@code DemandFactorCalculator} used to calculate demand factors by comparing base and variant skim matrices for given origin-destination zones.
     *
     * @param params elasticities parameters object containing elasticity parameters for the given segment
     */
    public DemandFactorCalculator(ElasticitiesParameters params, Matrices matrices) {
        this.params = params;
        this.matrices = matrices;

        ElasticityEntry.readAllEntries(params.getFile())
            .forEach(entry -> {
                entriesBySegmentAndCluster.computeIfAbsent(entry.segment(), k -> new HashMap<>());
                entriesBySegmentAndCluster.get(entry.segment()).computeIfAbsent(entry.cluster(), k -> new EnumMap<>(SkimType.class))
                    .put(entry.skimType(), entry);
            });

        if (entriesBySegmentAndCluster.isEmpty()) {
            throw new IllegalArgumentException("No elasticity entries found for segment " + params.getSegment());
        }
    }

    /**
     * Creates a demand matrix multiplier based on the base and variant skim matrices.
     *
     * @param base the base skim matrix
     * @param variant the variant skim matrix
     * @return a {@link DemandMatrixMultiplier} that can be used to multiply demand matrices
     */
    public Multiplier createMultiplier(Map<String, double[]> base, Map<String, double[]> variant) {
        return new Multiplier(base, variant);
    }

    /**
     * Compute the cluster, i.e. type of relation (international or national) based on the origin and destination zones.
     */
    private String computeCluster(String fromZoneNo, String toZoneNo) {

        String a = matrices.getZones().getCluster(fromZoneNo);
        String b = matrices.getZones().getCluster(toZoneNo);

        // Only CH and CH are considered as domestic traffic
        // Others are international
        // TODO: What about GG Grenzgebiete?

        return Objects.equals(a, b) && Objects.equals(a, "CH") ? "1" : "2";
    }

    private double computeElasticity(ElasticityEntry e, double ax, double bx) {
        return Math.max(e.min(), Math.min(e.max(), e.elasticity0() + e.a() * ax + e.b() * bx));
    }

    private double computeFactor(double[] variantValues, double[] baseValues, int idx, double e) {

        if (idx == UmlegoSkimCalculator.NTR_IDX) {
            // For NTR, a different calculation is applied
            return Math.pow((variantValues[idx] + params.getTransferOffset()) / (baseValues[idx] + params.getTransferOffset()), e);
        }

        if (baseValues[idx] == 0) {
            // If the base value is zero, we cannot compute a factor, return 1
            return 1;
        }

        return Math.pow(variantValues[idx] / baseValues[idx], e);
    }

    public final class Multiplier implements DemandMatrixMultiplier {

        private final Map<String, double[]> base;
        private final Map<String, double[]> variant;

        /**
         * Store the computed factors for each target zone.
         */
        private final Map<String, double[]> factors = new LinkedHashMap<>();

        public Multiplier(Map<String, double[]> base, Map<String, double[]> variant) {
            this.base = base;
            this.variant = variant;
        }

        @Override
        public double getFactor(String fromZoneNo, String toZoneNo, int timeMin) {
            double[] baseValues = base.getOrDefault(toZoneNo, EMPTY);
            double[] variantValues = variant.getOrDefault(toZoneNo, EMPTY);

            if (baseValues == EMPTY || variantValues == EMPTY ) {
                return 1.0; // No data available, return neutral factor
            }

            String cluster = computeCluster(fromZoneNo, toZoneNo);

            double ax = Math.min(baseValues[UmlegoSkimCalculator.ADT_IDX], params.getAdtUB()) / 15;
            double bx = baseValues[UmlegoSkimCalculator.JRT_IDX] / 45;

            double FJRT = 0;
            double FADT = 0;
            double FNTR = 0;

            for (var segment : matrices.getSegments()) {
                var share = matrices.getShareMatrixValue(segment, fromZoneNo, toZoneNo);

                double eJRT = computeElasticity(entriesBySegmentAndCluster.get(segment).get(cluster).get(SkimType.JRT), ax, bx);
                FJRT += computeFactor(variantValues, baseValues, UmlegoSkimCalculator.JRT_IDX, eJRT) * share;

                double eADT = computeElasticity(entriesBySegmentAndCluster.get(segment).get(cluster).get(SkimType.ADT), ax, bx);
                FADT += computeFactor(variantValues, baseValues, UmlegoSkimCalculator.ADT_IDX, eADT) * share;

                double eNTR = computeElasticity(entriesBySegmentAndCluster.get(segment).get(cluster).get(SkimType.NTR), ax, bx);
                FNTR += computeFactor(variantValues, baseValues, UmlegoSkimCalculator.NTR_IDX, eNTR) * share;

            }

            factors.put(toZoneNo, new double[]{FJRT, FADT, FNTR});

            return FJRT * FADT * FNTR;
        }

        public BewertoWorkResult createResult(String fromZoneNo) {
            return new BewertoWorkResult(fromZoneNo, factors, this.base, this.variant);
        }
    }

}
