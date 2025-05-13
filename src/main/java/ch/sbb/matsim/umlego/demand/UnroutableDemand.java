package ch.sbb.matsim.umlego.demand;

import java.util.ArrayList;
import java.util.List;

public class UnroutableDemand {

    private final List<UnroutableDemandPart> parts = new ArrayList<>();

    public List<UnroutableDemandPart> getParts() {
        return this.parts;
    }

    public void addPart(UnroutableDemandPart part) {
        getParts().add(part);
    }

    public double sum() {
        return getParts().stream().mapToDouble(UnroutableDemandPart::demand).sum();
    }
}
