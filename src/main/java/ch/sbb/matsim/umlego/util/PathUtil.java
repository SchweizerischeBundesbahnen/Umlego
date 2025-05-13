package ch.sbb.matsim.umlego.util;

import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.io.IOUtils;

public final class PathUtil {

    private static final Logger LOG = LogManager.getLogger(PathUtil.class);

    private PathUtil() {
    }

    public static void ensureDir(String outputFolder) {
        ensureDir(outputFolder, OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
    }

    public static void ensureDir(String outputFolder, OverwriteFileSetting overwriteFileSetting) {
        File outputDir = new File(outputFolder);
        switch (overwriteFileSetting) {
            case failIfDirectoryExists:
                // the directory is not empty, we do not overwrite any
                // files!
                throw new RuntimeException(
                    "The output directory " + outputDir
                        + " (full path: "
                        + outputDir.getAbsolutePath()
                        + ")"
                        + " already exists and is not empty!"
                        + " Please either delete or empty the directory or"
                        + " configure the services via setOverwriteFileSetting()"
                        + " or the \"overwriteFiles\" parameter of the \"services\" config group.");
            case overwriteExistingFiles:
                System.out.flush();
                LOG.warn("###########################################################");
                LOG.warn("### WILL OVERWRITE FILES IN:");
                LOG.warn("### full path: " + outputDir.getAbsolutePath());
                LOG.warn("###########################################################");
                System.err.flush();
                break;
            case deleteDirectoryIfExists:
                System.out.flush();
                LOG.info("###########################################################");
                LOG.info("### WILL DELETE THE EXISTING OUTPUT DIRECTORY:");
                LOG.warn("### full path: " + outputDir.getAbsolutePath());
                LOG.info("###########################################################");
                System.out.flush();
                IOUtils.deleteDirectoryRecursively(outputDir.toPath());
                break;
            default:
                throw new RuntimeException("unknown setting " + overwriteFileSetting);
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

}
