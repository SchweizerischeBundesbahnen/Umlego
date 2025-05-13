package ch.sbb.matsim.umlego;

import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.closeConnection;
import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.connect;
import static ch.sbb.matsim.umlego.util.PathUtil.ensureDir;

import ch.sbb.matsim.umlego.config.ExitCode;
import ch.sbb.matsim.umlego.config.SaisonRunIDNotOnSnowflakeException;
import ch.sbb.matsim.umlego.config.UmlegoConfig;
import ch.sbb.matsim.umlego.config.UmlegoException;
import ch.sbb.matsim.umlego.matrix.ZoneNotFoundException;
import ch.sbb.matsim.umlego.metadata.MetadataDayRepository;
import ch.sbb.matsim.umlego.metadata.MetadataRepository;
import ch.sbb.matsim.umlego.util.RunId;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.io.RuntimeIOException;

abstract class AbstractMultiDateRunner {

    private static final Logger LOG = LogManager.getLogger(AbstractMultiDateRunner.class);

    private final UmlegoConfig config;

    private boolean isUmlegoValid;

    protected AbstractMultiDateRunner(UmlegoConfig config) {
        this.config = config;
    }

    protected UmlegoConfig getConfig() {
        return this.config;
    }

    protected RunId getRunId() {
        return this.config.runId();
    }

    public void run() {
        if (isRunIdExisting(getRunId())) {
            String msg = "There is already an existing run with RunId " + getRunId().getValue();
            throw new UmlegoException(msg, ExitCode.RUN_ID_ALREADY_EXISTS);
        }
        long startTime = System.currentTimeMillis();
        String timetableFile = downloadHafasTimetable(config.getHafasFolder());
        renameHafasFiles(config.getHafasFolder());
        writeMetadata(getRunId());

        backupInputFiles();

        for (int i = 0; i < getDates().size(); i++) {
            LocalDate targetDate = getDates().get(i);
            runDayWriteMetaData(targetDate, i, timetableFile);
        }

        long endTime = System.currentTimeMillis();
        writeMetadataRuntime(getRunId(), endTime - startTime);
    }

    private void runDayWriteMetaData(LocalDate targetDate, int index, String timetableFile) {
        if (!isValidDayRun(targetDate, timetableFile)) {
            return;
        }

        long startTime = System.currentTimeMillis();
        writeMetadata(getRunId(), targetDate);
        runDay(targetDate, index, timetableFile);
        boolean isDayValid = isUmlegoDayValid(getRunId(), targetDate);
        isUmlegoValid = isUmlegoValid && isDayValid;
        long endTime = System.currentTimeMillis();
        writeMetadataRuntime(getRunId(), targetDate, endTime - startTime);
    }

    private void runDay(LocalDate targetDate, int index, String timetableFile) {
        try {
            String saison = config.calendar().getCalendarDayRecord(targetDate).baseDemand();
            String saisonRunId = config.simbaRunId() + "_" + saison;
            LOG.info("Run for target date {} with saisonRunId {}", targetDate, saisonRunId);
            checkSaisonRunId(saisonRunId);
            prepareOutputFolder(targetDate, index);
            convertTimetableForDate(targetDate, index);
            runUmlegoForDate(targetDate, index);
            cleanUp(targetDate, index);
        } catch (IOException | ZoneNotFoundException e) {
            LOG.error("Failed for date: {}", targetDate, e);
        } catch (SaisonRunIDNotOnSnowflakeException e) {
            String saison = config.calendar().getCalendarDayRecord(targetDate).baseDemand();
            String saisonRunId = config.simbaRunId() + "_" + saison;
            LOG.error("Failed for date: {} because the SaisonRunId {} was not on snowflake", targetDate, saisonRunId, e);
        }
    }

    private boolean isValidDayRun(LocalDate targetDate, String timetableFile) {
        if (!getConfig().calendar().isDateInCalendar(targetDate)) {
            LOG.warn("Target date [{}] is not in calendar for year {}", targetDate, config.year());
            return false;
        }
        if (isDateAlreadyCalculated(targetDate, timetableFile)) {
            LOG.warn("Skipped calculation for target date [{}]. Combination with [{}] [{}] already exists.", targetDate, timetableFile, getConfig().simbaRunId());
            return false;
        }
        return true;
    }

    private void convertTimetableForDate(LocalDate targetDate, int index) throws IOException {
        String scheduleOutputFolder = getScheduleFolder(targetDate, index);
        ensureDir(scheduleOutputFolder);

        backupSchedule(scheduleOutputFolder, targetDate);

        LOG.info("Timetable conversion completed for date: {}", targetDate);
    }

    private void runUmlegoForDate(LocalDate targetDate, int index) throws IOException, ZoneNotFoundException, SaisonRunIDNotOnSnowflakeException {
        UmlegoRunnerFactory factory = new UmlegoRunnerFactory();
        UmlegoRunner runner = factory.createRunner(config, getScheduleFolder(targetDate, index), targetDate, index);
        runner.run();
    }

    /**
     * In both cases (local and cloud) Hafas2MATSim schedule works with local filesystem.
     * <p>
     * The schedule folder is the:
     * <li>output folder for the timetable conversion</li>
     * <li>input folder for then Umlegung.</li>
     *
     * @param targetDate day for the Umlegung
     * @param index targetDate's index
     * @return output folder
     */
    protected String getScheduleFolder(LocalDate targetDate, int index) {
        return Paths.get(getLocalOutputFolder(targetDate, index), "schedule").toString();
    }

    /**
     * Return dates from config.yaml.
     *
     * @return List<LocalDate>
     */
    protected List<LocalDate> getDates() {
        return getConfig().dates();
    }

    abstract void checkSaisonRunId(String saisonRunId);

    abstract boolean isDateAlreadyCalculated(LocalDate targetDate, String timetable);

    abstract void backupInputFiles();

    abstract void prepareOutputFolder(LocalDate date, int index) throws IOException;

    abstract String getLocalOutputFolder(LocalDate date, int index);

    abstract void cleanUp(LocalDate date, int index);

    abstract void writeMetadata(RunId runId);

    abstract void writeMetadata(RunId runId, LocalDate targetDate);

    abstract void writeMetadataRuntime(RunId runId, long duration);

    abstract void writeMetadataRuntime(RunId runId, LocalDate targetDate, long duration);

    /**
     * Subclasses should override this to define how to download timetable files.
     *
     * @param hafasFolder where to store timetable files.
     * @return timetable that was downloaded.
     */
    String downloadHafasTimetable(String hafasFolder) {
        return "";
    }

    /**
     * Subclasses should override this to define how MATSim schedule files are backuped.
     *
     * @param localScheduleFolder input folder of files to back up.
     * @param targetDate date used to define the remote backup folder.
     */
    void backupSchedule(String localScheduleFolder, LocalDate targetDate) {
    }

    /**
     * SBB's INFO+ HAFAS outputs use non-standard filenames. We rename them here for the converter to work.
     *
     * @param hafasFolder where to store timetable files.
     */
    private void renameHafasFiles(String hafasFolder) {
        try {
            LOG.info("Renaming files in hafasFolder [{}].", hafasFolder);
            Path fplanSbbFile = Paths.get(hafasFolder, "fplansbb");
            if (fplanSbbFile.toFile().exists()) {
                Files.move(fplanSbbFile, Paths.get(hafasFolder, "FPLAN"));
                Files.move(Paths.get(hafasFolder, "betrieb"), Paths.get(hafasFolder, "BETRIEB_DE"));
                Files.move(Paths.get(hafasFolder, "bfkoord"), Paths.get(hafasFolder, "BFKOORD_WGS"));
                Files.move(Paths.get(hafasFolder, "bitfield"), Paths.get(hafasFolder, "BITFELD"));
                Files.move(Paths.get(hafasFolder, "umsteigb"), Paths.get(hafasFolder, "UMSTEIGB"));
                Files.move(Paths.get(hafasFolder, "metabhf"), Paths.get(hafasFolder, "METABHF"));
                Files.move(Paths.get(hafasFolder, "eckdaten"), Paths.get(hafasFolder, "ECKDATEN"));
            }
        } catch (IOException e) {
            throw new RuntimeIOException(e);
        }
    }

    protected void printHeapSizes() {
        // Retrieve the MemoryMXBean
        MemoryMXBean memoryMxBean = ManagementFactory.getMemoryMXBean();
        // Retrieve the MemoryUsage for the heap
        MemoryUsage heapMemoryUsage = memoryMxBean.getHeapMemoryUsage();

        // Get initial and maximum heap size
        long initialHeapSize = heapMemoryUsage.getInit();
        long maxHeapSize = heapMemoryUsage.getMax();

        // Print the values to the console
        LOG.info("Initial Heap Size (Xms): {} {}", initialHeapSize / (1024 * 1024), "MB");
        LOG.info("Maximum Heap Size (Xmx): {} {}", maxHeapSize / (1024 * 1024), "MB");
    }

    private boolean isUmlegoDayValid(RunId runId, LocalDate targetDate) {
        Connection connection = connect();
        MetadataDayRepository repository = new MetadataDayRepository();
        boolean isValid = repository.isUmlegoValid(connection, runId, targetDate);
        closeConnection(connection);
        return isValid;
    }

    protected boolean isRunIdExisting(RunId runId) {
        Connection connection = connect();
        MetadataRepository repository = new MetadataRepository();
        boolean isExisting = repository.checkForRunId(connection, runId.getValue());
        closeConnection(connection);

        return isExisting;
    }
}
