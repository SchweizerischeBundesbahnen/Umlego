package ch.sbb.matsim.bewerto;

import ch.sbb.matsim.bewerto.config.BewertoParameters;
import org.github.gestalt.config.Gestalt;
import org.github.gestalt.config.builder.GestaltBuilder;
import org.github.gestalt.config.source.ClassPathConfigSourceBuilder;
import org.github.gestalt.config.source.FileConfigSourceBuilder;
import org.github.gestalt.config.source.SystemPropertiesConfigSourceBuilder;
import org.github.gestalt.config.yaml.YamlModuleConfigBuilder;
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

        Gestalt config = new GestaltBuilder()
                .addSource(ClassPathConfigSourceBuilder.builder().setResource("/bewerto.yaml").build())
                .addSource(FileConfigSourceBuilder.builder().setPath(configPath).build())
                .addSource(SystemPropertiesConfigSourceBuilder.builder().build())
                .addModuleConfig(YamlModuleConfigBuilder.builder().build())
                .build();

        config.loadConfigs();

        BewertoParameters params = config.getConfig("bewerto", BewertoParameters.class);

        Bewerto bewerto = new Bewerto(params);
        bewerto.run();

        return 0;
    }
}
