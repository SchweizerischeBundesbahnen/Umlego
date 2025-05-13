package ch.sbb.matsim.umlego.it;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;
import ch.sbb.matsim.umlego.writers.UmlegoListenerInterface;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UmlegoITListener implements UmlegoListenerInterface {

    public String fromZone;
    public String toZone;
    public List<FoundRoute> routes;

    public UmlegoITListener(String fromZone, String toZone) {
        this.fromZone = fromZone;
        this.toZone = toZone;
        this.routes = new ArrayList<FoundRoute>();
    }

    private static final Logger LOG = LogManager.getLogger(UmlegoITListener.class);

    @Override
    public void processRoute(String origZone, String destZone, FoundRoute route) {
        if (Objects.equals(this.fromZone, origZone) && Objects.equals(this.toZone, destZone)) {
            this.routes.add(route);
        }
    }
}