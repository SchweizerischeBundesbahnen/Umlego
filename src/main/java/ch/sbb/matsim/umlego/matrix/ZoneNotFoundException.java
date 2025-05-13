package ch.sbb.matsim.umlego.matrix;

public class ZoneNotFoundException extends Exception {

    public ZoneNotFoundException(String zone) {
        super("Zone " + zone + " not found.");
    }
}
