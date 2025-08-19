package ch.sbb.matsim.umlego.workflows.bewerto.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The {@code ElasticitiesParameters} class represents configuration for elasticity calculations.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class ElasticitiesParameters {

    /**
     * Path to elasticities data file.
     */
    private String file;

    /**
     * Segment identifier for elasticities.
     */
    private String segment;

    /**
     * Offset for transfer elasticity calculations. Needs to be larger than 0.0.
     */
    private double transferOffset = 0.5;

    /**
     * Upper bound for adaption time in the calculation. Default is 90.0 minutes.
     */
    private double adtUB = 90.0;

    @Override
    public String toString() {
        return "ElasticitiesParameters{" +
            "file='" + file + '\'' +
            ", segment='" + segment + '\'' +
            ", transferOffset=" + transferOffset +
            ", adtUB=" + adtUB +
            '}';
    }
}
