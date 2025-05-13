package ch.sbb.matsim.umlego.util;

import ch.sbb.matsim.umlego.config.UmlegoConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.utils.io.IOUtils;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;

import static ch.sbb.matsim.umlego.config.hadoop.FileSystemUtil.exists;
import static ch.sbb.matsim.umlego.config.hadoop.FileSystemUtil.getRootDir;

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

    /**
     * Returns output folder in local context
     */
    public static String getLocalOutputFolder(RunId runId, LocalDate date) {
        String dateSt = date.format(DateTimeFormatterUtil.ISO_FORMATTER_COMPACT);
        return "/tmp/" + runId.getValue() + "/" + dateSt;
    }

    public static String getLocalOutputFolder(UmlegoConfig config, LocalDate date, int index) {
        String targetDateStr = date.format(DateTimeFormatterUtil.DATE_FORMATTER);
        return Paths.get(config.outputFolder(), index + "_output_" + targetDateStr).toString();
    }

    /**
     * Returns output folder for converted MAtSime timetable.
     */
    public static String getLocalScheduleOutputFolder(RunId runId, LocalDate date) {
        return getLocalOutputFolder(runId, date) + "/" + "schedule";
    }

    /**
     * Returns output folder in remote (Azure) context
     */
    public static String getRemoteOutputFolder(RunId runId, int year, LocalDate date) {
        String dateSt = date.format(DateTimeFormatterUtil.ISO_FORMATTER_COMPACT);
        return year + "/" + runId.getValue() + "/output/" + dateSt;
    }


    public static String getInputBackupFolder(RunId runId, int year) {
        return year + "/" + runId.getValue() + "/input";
    }

    public static String getInputGlobalFolder() {
        return "input";
    }

    public static String getInputYearFolder(int year) {
        return year + "/input";
    }

    public static String getAbsoluteGlobalFileName(String fileName, int year, String inputFolder) {
        String filePath = getAlternativeFileName(year, inputFolder, fileName);
        return (filePath != null) ? filePath : getRootDir() + PathUtil.getInputGlobalFolder() + "/" + fileName;
    }

    public static String getAbsoluteYearFileName(String fileName, int year, String inputFolder) {
        String filePath = getAlternativeFileName(year, inputFolder, fileName);
        return (filePath != null) ? filePath : getRootDir() + PathUtil.getInputYearFolder(year) + "/" + fileName;
    }

    public static String getAbsoluteFactorDirectory(int year, String inputFolder) {
        String path = getAlternativeFileName(year, inputFolder, "factors");
        return (path != null) ? path : getRootDir() + PathUtil.getInputYearFolder(year) + "/factors";
    }

    /**
     * Checks whether the input folder exists in the filesystem.
     *
     * @param year The timetable year
     * @param inputFolder The input folder
     * @return boolean
     */
    public static boolean doesInputFolderExist(int year, String inputFolder) {
        String folderPath = getRootDir() + getInputYearFolder(year) + "/" + inputFolder;
        return exists(folderPath);
    }

    /**
     * If input_folder is defined, check if there exists the file "fileName".
     *
     * @param fileName
     * @return boolean
     */
    private static  String getAlternativeFileName(int year, String inputFolder, String fileName) {
        if (StringUtils.isNotEmpty(inputFolder)) {
            String filePath = getRootDir() + PathUtil.getInputYearFolder(year) + "/" + inputFolder + "/" + fileName;
            if (exists(filePath)) {
                return filePath;
            }
            LOG.warn("Did not found the file {} default file will be used instead.", filePath);
        } else {
            LOG.warn("No input folder given, default {} file will be used instead.", fileName);
        }
        return null;
    }



}
