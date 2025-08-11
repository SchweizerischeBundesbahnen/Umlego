package ch.sbb.matsim.umlego.workflows.bewerto.config;

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

    /**
     * Offset for transfer elasticity calculations. Needs to be larger than 0.0.
     */
    private double transferOffset = 0.5;

    /**
     * Upper bound for adaption time in the calculation. Default is 90.0 minutes.
     */
    private double adtUB = 90.0;

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

    public ElasticitiesParameters setTransferOffset(double transferOffset) {
        this.transferOffset = transferOffset;
        return this;
    }

    public double getTransferOffset() {
        return transferOffset;
    }

    public ElasticitiesParameters setAdtUB(double adtUB) {
        this.adtUB = adtUB;
        return this;
    }

    public double getAdtUB() {
        return adtUB;
    }


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
