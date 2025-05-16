package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.bewerto.config.BewertoParameters;

/**
 * The {@code Bewerto} class serves the core functionality of the Bewerto application.
 */
public final class Bewerto implements Runnable {

    private final BewertoParameters params;

    /**
     * The main constructor for the Bewerto class.
     */
    public Bewerto(BewertoParameters params) {
        this.params = params;
    }

    @Override
    public void run() {

        System.out.println(params);



    }
}
