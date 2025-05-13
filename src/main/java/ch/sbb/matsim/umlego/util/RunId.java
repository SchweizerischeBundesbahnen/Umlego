package ch.sbb.matsim.umlego.util;

public class RunId {

    private final String value;

    public RunId(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public int getYear() {
        String year = getValue().substring(0, 4);
        return Integer.parseInt(year);
    }
}
