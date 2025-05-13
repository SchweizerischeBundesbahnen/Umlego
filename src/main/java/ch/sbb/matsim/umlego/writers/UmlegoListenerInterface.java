package ch.sbb.matsim.umlego.writers;

import ch.sbb.matsim.umlego.Umlego.FoundRoute;

public interface UmlegoListenerInterface  {

    void processRoute(String origZone, String destZone, FoundRoute route);

}