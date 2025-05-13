package ch.sbb.matsim.umlego;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.Level;

import java.io.File;

public final class UmlegoLogger {

    private static String currentOutputFolder;
    private static boolean isLoggerInitialized = false;

    private UmlegoLogger() {
    }

    public static void setOutputFolder(String outputFolder) {
        currentOutputFolder = outputFolder;
        configureExperimentLogger();
    }

    public static void configureDefaultLogger() {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            ConfigurationBuilder<BuiltConfiguration> configurationBuilder = ConfigurationBuilderFactory.newConfigurationBuilder();

            LayoutComponentBuilder layoutBuilder = configurationBuilder
                    .newLayout("PatternLayout")
                    .addAttribute("pattern", "%d{ISO8601} [%t] %5p %C{1}:%L %m%n");

            AppenderComponentBuilder consoleAppenderBuilder = configurationBuilder
                    .newAppender("stdout", "Console")
                    .add(layoutBuilder);

            configurationBuilder.add(consoleAppenderBuilder);
            configurationBuilder.add(configurationBuilder.newRootLogger(Level.INFO).add(configurationBuilder.newAppenderRef("stdout")));

            context.start(configurationBuilder.build());

            isLoggerInitialized = true;
        } catch (Exception e) {
            // Use the logger to capture initialization errors instead of printStackTrace
            Logger logger = LogManager.getLogger(UmlegoLogger.class);
            logger.error("Failed to configure default logger", e);
        }
    }

    // Method to configure the logger for the current experiment
    private static void configureExperimentLogger() {
        if (currentOutputFolder == null) {
            throw new IllegalStateException("Output folder must be set before configuring the logger.");
        }

        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            ConfigurationBuilder<BuiltConfiguration> configurationBuilder = ConfigurationBuilderFactory.newConfigurationBuilder();

            LayoutComponentBuilder layoutBuilder = configurationBuilder
                    .newLayout("PatternLayout")
                    .addAttribute("pattern", "%d{ISO8601} [%t] %5p %C{1}:%L %m%n");

            String logFilePath = currentOutputFolder + "/umlego.log";
            new File(logFilePath).getParentFile().mkdirs();

            AppenderComponentBuilder fileAppenderBuilder = configurationBuilder
                    .newAppender("logfile", "File")
                    .addAttribute("fileName", logFilePath)
                    .add(layoutBuilder);

            AppenderComponentBuilder consoleAppenderBuilder = configurationBuilder
                    .newAppender("stdout", "Console")
                    .add(layoutBuilder);

            configurationBuilder.add(fileAppenderBuilder);
            configurationBuilder.add(consoleAppenderBuilder);

            configurationBuilder
                    .add(configurationBuilder.newRootLogger(Level.DEBUG)
                    .add(configurationBuilder.newAppenderRef("logfile"))
                    .add(configurationBuilder.newAppenderRef("stdout")));

            context.start(configurationBuilder.build());

            isLoggerInitialized = true;

        } catch (Exception e) {
            // Use the logger to capture initialization errors instead of printStackTrace
            Logger logger = LogManager.getLogger(UmlegoLogger.class);
            logger.error("Failed to configure experiment logger", e);
        }
    }

    public static Logger getLogger(Class<?> clazz) {
        if (!isLoggerInitialized) configureDefaultLogger();
        return LogManager.getLogger(clazz);
    }
}
