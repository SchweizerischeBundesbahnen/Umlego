package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.bewerto.config.BewertoParameters;
import ch.sbb.matsim.umlego.UmlegoRunner;
import ch.sbb.matsim.umlego.config.UmlegoParameters;
import org.github.gestalt.config.Gestalt;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "bewerto",
        description = "Bewerto command line tool",
        mixinStandardHelpOptions = true
)
public final class BewertoRunner implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-c", "--config"},
            description = "Path to the configuration file",
            required = true
    )
    private Path configPath;

    public static void main(String[] args) {
        new CommandLine(new BewertoRunner())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
    }

    @Override
    public Integer call() throws Exception {

        Gestalt config = UmlegoRunner.loadConfig(configPath);

        BewertoParameters bewertoParameters = config.getConfig("bewerto", BewertoParameters.class);
        UmlegoParameters umlegoParameters = config.getConfig("umlego", UmlegoParameters.class);

        Bewerto bewerto = new Bewerto(bewertoParameters, umlegoParameters);
        bewerto.run();

        return 0;
    }
}
