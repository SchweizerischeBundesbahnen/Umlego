package ch.sbb.matsim.umlego.log;

import org.apache.logging.log4j.core.layout.PatternLayout ;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.io.IOException;

/**
 * Class replaces current HdfsAppender with new HdfsAppender writing to a different log file.
 */
public class LogfileSwitcher {

    private final static String HDFS_APPENDER_NAME = "HdfsAppender";

    private final static String DEFAULT_LAYOUT = "%d [%t] %-5level: %msg%n%throwable";

    public static void setLogfile(String logfile) {
        try {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration config = context.getConfiguration();
            LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME);

            Appender oldAppender = loggerConfig.getAppenders().get(HDFS_APPENDER_NAME);
            Layout layout = (oldAppender != null) ? oldAppender.getLayout(): PatternLayout.createDefaultLayout();
            Appender newAppender = HdfsAppender.createAppender(HDFS_APPENDER_NAME, layout, logfile);

            if (oldAppender != null) oldAppender.stop();
            loggerConfig.removeAppender(HDFS_APPENDER_NAME);

            newAppender.start();
            loggerConfig.addAppender(newAppender, loggerConfig.getLevel(), null);
        } catch (IOException e) {
            throw new RuntimeException("Error while replacing HdfsAppender", e);
        }
    }
}
