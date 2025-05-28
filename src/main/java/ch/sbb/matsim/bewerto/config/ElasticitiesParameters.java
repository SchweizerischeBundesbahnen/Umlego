package ch.sbb.matsim.bewerto.config;

/**
 * The {@code ElasticitiesParameters} class represents configuration for elasticity calculations.
 */
public final class ElasticitiesParameters {

    /**
     * Path to elasticities data file.
     */
    private String file;

    /**
     * Segment identifier for elasticities.
     */
    private String segment;

    public String getFile() {
        return file;
    }

    public ElasticitiesParameters setFile(String file) {
        this.file = file;
        return this;
    }

    public String getSegment() {
        return segment;
    }

    public ElasticitiesParameters setSegment(String segment) {
        this.segment = segment;
        return this;
    }

    @Override
    public String toString() {
        return "ElasticitiesParameters{" +
                "file='" + file + '\'' +
                ", segment='" + segment + '\'' +
                '}';
    }
}
