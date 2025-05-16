package ch.sbb.matsim.umlego.it;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;
import ch.sbb.matsim.umlego.UmlegoListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UmlegoITListener implements UmlegoListener {

    public String fromZone;
    public String toZone;
    public List<FoundRoute> routes;

    public UmlegoITListener(String fromZone, String toZone) {
        this.fromZone = fromZone;
        this.toZone = toZone;
        this.routes = new ArrayList<FoundRoute>();
    }

    @Override
    public void processRoute(String origZone, String destZone, FoundRoute route) {
        if (Objects.equals(this.fromZone, origZone) && Objects.equals(this.toZone, destZone)) {
            this.routes.add(route);
        }
    }
}