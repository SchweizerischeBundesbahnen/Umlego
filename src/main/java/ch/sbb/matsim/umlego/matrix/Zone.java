package ch.sbb.matsim.umlego.matrix;

public class Zone {

    private String no;
    private String name;
    private String cluster;

    public Zone(String no, String name, String cluster) {
        this.name = name;
        this.no = no;
        this.cluster = cluster;
    }

    public String getElasticityCluster() {
        return cluster;
    }

    public String getName() {
        return name;
    }

    public String getNo() {
        return no;
    }

}
