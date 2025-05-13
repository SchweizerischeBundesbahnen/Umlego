package ch.sbb.matsim.umlego;

import static ch.sbb.matsim.umlego.config.hadoop.FileSystemUtil.getRootDir;
import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.closeConnection;
import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.connect;
import static ch.sbb.matsim.umlego.metadata.MetadataDayKey.BASE_DEMAND;
import static ch.sbb.matsim.umlego.metadata.MetadataDayKey.CORRECTIONS;
import static ch.sbb.matsim.umlego.metadata.MetadataDayKey.WEEKDAY;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.CALENDAR_VERSION;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.CONNECTIONS_VERSION;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.FACTORS_VERSION;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.MANUAL;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.TIMESTAMP;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.ZONES_VERSION;
import static ch.sbb.matsim.umlego.util.DateTimeFormatterUtil.format;
import static ch.sbb.matsim.umlego.util.DateTimeFormatterUtil.getAbbreviatedWeekday;
import static ch.sbb.matsim.umlego.util.FileUtil.copyDirectory;
import static ch.sbb.matsim.umlego.util.FileUtil.copyFile;
import static ch.sbb.matsim.umlego.util.FileUtil.createDirectory;
import static ch.sbb.matsim.umlego.util.MetadataUtil.insertDirectoryMetadata;
import static ch.sbb.matsim.umlego.util.MetadataUtil.insertFileMetadata;
import static ch.sbb.matsim.umlego.util.PathUtil.*;

import ch.sbb.matsim.umlego.config.*;
import ch.sbb.matsim.umlego.config.UmlegoCalendar.UmlegoCalendarDay;
import ch.sbb.matsim.umlego.config.cli.UmlegoCommandLineParser;
import ch.sbb.matsim.umlego.config.hadoop.FileSystemUtil;
import ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig;
import ch.sbb.matsim.umlego.log.LogfileSwitcher;
import ch.sbb.matsim.umlego.metadata.MetadataDayRepository;
import ch.sbb.matsim.umlego.metadata.MetadataDayKey;
import ch.sbb.matsim.umlego.metadata.MetadataKey;
import ch.sbb.matsim.umlego.metadata.MetadataRepository;
import ch.sbb.matsim.umlego.readers.jdbc.TimesliceRepository;
import ch.sbb.matsim.umlego.timetable.HafasTimetableDownloader;
import ch.sbb.matsim.umlego.timetable.MATSimTimetableBackuper;
import ch.sbb.matsim.umlego.util.DateTimeFormatterUtil;
import ch.sbb.matsim.umlego.util.PathUtil;
import ch.sbb.matsim.umlego.util.RunId;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The UmlegoCloudMultiDateRunner executes the Umlegung in a Pod on OpenShift. It's meant to run in a cloud environment.
 * Configuration is mostly done by convention.
 *
 * <li>HAFAS Timetable is downloaded and unzipped from INFO+ sFTP server</li>
 * <li>demand matrices are read from Snwoflake database tables</li>
 * <li>correction factor files must be available on Azure Blob Storage</li>
 * <li>other input files (zonesLookup, anbindungen, calendar) must be available on Azure Blob Storage</li>
 *
 * <li>output (volume, skims, unroutable_demand) is written into Snowflake tables</li>
 */
public class UmlegoCloudMultiDateRunner extends AbstractMultiDateRunner {

    private static final Logger LOG = LogManager.getLogger(UmlegoCloudMultiDateRunner.class);

    public static void main(String[] args) {
        try {
            UmlegoCommandLineParser parser = new UmlegoCommandLineParser();
            parser.parse(args);

            UmlegoConfig config = UmlegoConfig.create(
                    parser.getRunId(),
                    parser.getSimbaRunId(),
                    parser.getYear(),
                    parser.isCron(),
                    parser.getTargetDates(),
                    parser.getInputFolder());

            UmlegoCloudMultiDateRunner runner = new UmlegoCloudMultiDateRunner(config);
            runner.checkInputs();
            runner.printHeapSizes();
            runner.run();
            runner.checkValidAuto();
        } catch (NoRunNeededException e) {
            System.exit(ExitCode.SUCCESS.getCode());
        } catch (UmlegoException e) {
            LOG.error(e.getMessage(), e.getCause());
            System.exit(e.getExitCode());
        } catch (Exception e) {
            LOG.error("An unexpected error occurred.", e);
            System.exit(ExitCode.FAILED.getCode());
        }
    }

    /**
     * Constructor.
     *
     * @param config partially hard-coded when running in the cloud.
     */
    public UmlegoCloudMultiDateRunner(UmlegoConfig config) {
        super(config);
    }

    protected void checkInputs() {
        checkSaisonBaseId();
        checkInputFolder();
        // other checks are implemented further im code, once the calendar and timetables are loaded
    }

    String getSaisonBaseId() {
        return getConfig().simbaRunId();
    }

    private int year() {
        return getConfig().year();
    }

    private boolean isCron() {
        return getConfig().isCron();
    }

    private String inputFolder() {
        return getConfig().inputFolder();
    }


    /**
     * Dates already calculated before (in combination with Saison-ID and timetable) are skipped
     *
     * @param targetDate
     * @return boolean
     */
    @Override
    boolean isDateAlreadyCalculated(LocalDate targetDate, String timetable) {
        Connection connection = SnowflakeConfig.connect();
        MetadataDayRepository repository = new MetadataDayRepository();
        boolean returnValue = repository.findByTargetDateSaisonBaseTimetable(connection, targetDate, getConfig().simbaRunId(), timetable);
        SnowflakeConfig.closeConnection(connection);
        return returnValue;
    }

    /**
     * In the cloud, we have to prepare two kind of output folders:
     * <li>remote output folder on Azure Blob Storage, used for storing log an converted schedules.</li>
     * <li>local output folder in /tmp, needed by HafasConverter code.</li>
     *
     * @param date
     * @param index
     * @throws IOException
     */
    @Override
    protected void prepareOutputFolder(LocalDate date, int index) throws IOException {
        LOG.info("Preparing output folder");
        String remoteOutputFolder = PathUtil.getRemoteOutputFolder(getRunId(), year(), date);

        LogfileSwitcher.setLogfile(getRootDir() + remoteOutputFolder + "/umlego.log");

        FileSystem fs = FileSystemUtil.getFileSystem();
        fs.mkdirs(new Path(getRootDir() + remoteOutputFolder));
    }

    /**
     * Backs up input files from original location on Blob Storage to a run-based location.
     * They only need to be backuped ONCE per run.
     *
     * @throws IOException if an I/O error occurs during file operations or database interactions
     */
    @Override
    void backupInputFiles() {
        try {
            LOG.info("Backing up input files");

            String backupFolder = getRootDir() + PathUtil.getInputBackupFolder(getRunId(), year());

            createDirectory(backupFolder);
            copyFile(getAbsoluteGlobalFileName("anbindungen.csv", year(), inputFolder()), backupFolder);
            copyFile(getAbsoluteGlobalFileName("zones.csv", year(), inputFolder()), backupFolder);
            copyFile(getAbsoluteYearFileName("calendar.csv", year(), inputFolder()), backupFolder);
            copyDirectory(getAbsoluteFactorDirectory(year(), inputFolder()), backupFolder);

            writeLastModificationTime();
        } catch (IOException e) {
            throw new RuntimeException("Error while backing up input files", e);
        }
    }

    /**
     * Inserts metadata for the input files into the metadata table.
     *
     * @throws RuntimeException if an I/O error occurs during file operations or database interactions
     */
    private void writeLastModificationTime() {
        try (Connection connection = connect()) {
            String runIdValue = getRunId().getValue();
            FileSystem fs = FileSystemUtil.getFileSystem();
            MetadataRepository repo = new MetadataRepository();

            insertFileMetadata(fs, getRootDir() + PathUtil.getInputGlobalFolder() + "/anbindungen.csv",
                    repo, connection, runIdValue, CONNECTIONS_VERSION);

            insertFileMetadata(fs, getRootDir() + PathUtil.getInputGlobalFolder() + "/zones.csv",
                    repo, connection, runIdValue, ZONES_VERSION);

            insertFileMetadata(fs, getRootDir() + PathUtil.getInputYearFolder(year()) + "/calendar.csv",
                    repo, connection, runIdValue, CALENDAR_VERSION);

            insertDirectoryMetadata(fs, getRootDir() + PathUtil.getInputYearFolder(year()) + "/factors",
                    repo, connection, runIdValue, FACTORS_VERSION);

        } catch (SQLException | IOException e) {
            throw new RuntimeException("Failed to write metadata for input files. ", e);
        }
    }

    @Override
    String getLocalOutputFolder(LocalDate date, int index) {
        String dateSt = date.format(DateTimeFormatterUtil.ISO_FORMATTER_COMPACT);
        return "/tmp/" + getRunId().getValue() + "/" + dateSt;
    }

    @Override
    String downloadHafasTimetable(String hafasFolder) {
        HafasTimetableDownloader downloader = new HafasTimetableDownloader();
        return downloader.downloadAndExtractFile(hafasFolder, getRunId(), year(), isCron(), getSaisonBaseId());
    }

    @Override
    void backupSchedule(String localScheduleFolder, LocalDate targetDate) {
        MATSimTimetableBackuper.create().backup(localScheduleFolder, getRunId(), year(), targetDate);
    }

    /**
     * Remove all generated files from local filesystem to avoid to submerge the filesystem when
     * i.e. running a whole year.
     *
     * @param targetDate
     * @param index
     */
    @Override
    protected void cleanUp(LocalDate targetDate, int index) {
        File dir = new File(getScheduleFolder(targetDate, index));
        try {
            FileUtils.deleteDirectory(dir);
        } catch (IOException e) {
            LOG.warn("Error while deleting directory {}", dir, e);
        }
    }

    @Override
    void writeMetadata(RunId runId){
        Connection connection = connect();
        MetadataRepository repo = new MetadataRepository();

        ZonedDateTime timestamp = ZonedDateTime.now();

        repo.insertMetadata(connection, runId.getValue(), TIMESTAMP, format(timestamp));
        repo.insertMetadata(connection, runId.getValue(), MANUAL, !isCron());

        closeConnection(connection);
    }

    @Override
    void writeMetadata(RunId runId, LocalDate targetDate) {
        UmlegoCalendarDay calendarDay = getConfig().calendar().getCalendarDayRecord(targetDate);

        if (calendarDay != null) {
            Connection connection = connect();
            MetadataDayRepository repo = new MetadataDayRepository();

            repo.insertMetadata(connection, runId.getValue(), targetDate, MetadataDayKey.TIMESTAMP, format(ZonedDateTime.now()));
            repo.insertMetadata(connection, runId.getValue(), targetDate, CORRECTIONS, String.join(", ", calendarDay.correctionFactors()));
            repo.insertMetadata(connection, runId.getValue(), targetDate, BASE_DEMAND, calendarDay.baseDemand());
            repo.insertMetadata(connection, runId.getValue(), targetDate, WEEKDAY, getAbbreviatedWeekday(targetDate));

            closeConnection(connection);
        }
    }

    @Override
    void writeMetadataRuntime(RunId runId, long duration) {
        Connection connection = connect();
        MetadataRepository repo = new MetadataRepository();
        repo.insertMetadata(connection, runId.getValue(), MetadataKey.RUNTIME, duration / 1000);
        closeConnection(connection);
    }

    @Override
    void writeMetadataRuntime(RunId runId, LocalDate targetDate, long duration) {
        Connection connection = connect();
        MetadataDayRepository repo = new MetadataDayRepository();
        repo.insertMetadata(connection, runId.getValue(), targetDate, MetadataDayKey.RUNTIME, duration / 1000);
        closeConnection(connection);
    }

    /**
     * Checks if there are any days where the automatic validation has failed.
     */
    private void checkValidAuto() {
        Connection connection = connect();
        try {
            MetadataDayRepository repository = new MetadataDayRepository();
            String invalidDates = repository.getInvalidTargetDates(connection, getConfig().runId().getValue());
            if (!invalidDates.isEmpty()) {
                LOG.info("Found umlego with valid_auto = false for dates: {}. Exiting with code 20.", invalidDates);
                throw new UmlegoException("Automatic validation failed", ExitCode.AUTOMATIC_VALIDATION_FAILED);
            }
        } finally {
            closeConnection(connection);
        }
    }

    private void checkSaisonBaseId() {
        try {
            Connection connection = SnowflakeConfig.connect();
            boolean isSaisonBaseIdOnSnowflake = TimesliceRepository.isSaisonBaseIdOnSnowflake(connection, this.getSaisonBaseId());
            SnowflakeConfig.closeConnection(connection);
            if (!isSaisonBaseIdOnSnowflake) {
                String msg = "The the saisonBaseId " + this.getSaisonBaseId() + " was not found in the TGM_ZEITSCHEIBE table on snowflake.";
                throw new UmlegoException(msg, ExitCode.SAISON_BASE_ID_NOT_FOUND);
            } else {
                LOG.info("The the saisonBaseId {} exist in the TGM_ZEITSCHEIBE table on snowflake.", this.getSaisonBaseId());
            }
        } catch (SQLException e) {
            String msg = "SQL error while querying the snowflake database.";
            throw new UmlegoException(msg, e, ExitCode.SQL_ERROR);
        }
    }

    @Override
    protected void checkSaisonRunId(String saisonRunId) throws SaisonRunIDNotOnSnowflakeException {
        LOG.info("Check saisonRunId {}", saisonRunId);
        try {
            Connection connection = SnowflakeConfig.connect();
            boolean isSaisonRunIdOnSnowflake = TimesliceRepository.isSaisonRunIdOnSnowflake(connection, saisonRunId);
            SnowflakeConfig.closeConnection(connection);
            if (isSaisonRunIdOnSnowflake) {
                String msg = "Found saison run id " + saisonRunId + " in the TGM_ZEITSCHEIBE table on snowflake.";
                LOG.info(msg);
            } else {
                String msg = "Not found saison run id " + saisonRunId + " in the TGM_ZEITSCHEIBE table on snowflake.";
                LOG.error(msg);
                throw new SaisonRunIDNotOnSnowflakeException(msg);
            }
        } catch (SQLException e) {
            String msg = "SQLError while querying the saison run id : " + saisonRunId;
            LOG.error(msg, e);
            throw new UmlegoException(msg, e, ExitCode.SQL_ERROR);
        }
    }

    private void checkInputFolder() {
        String inputFolder = this.getConfig().inputFolder();
        if (StringUtils.isNotEmpty(inputFolder) && !PathUtil.doesInputFolderExist(this.getConfig().year(), inputFolder)) {
            String errorMessage = "Input folder " + inputFolder + " does not exist.";
            throw new UmlegoException(errorMessage, ExitCode.INVALID_INPUT_FOLDER);
        }
    }
}
