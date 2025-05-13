package ch.sbb.matsim.umlego.config;

import static ch.sbb.matsim.umlego.config.hadoop.FileSystemUtil.getFileSystem;
import static ch.sbb.matsim.umlego.config.hadoop.FileSystemUtil.getRootDir;

import ch.sbb.matsim.umlego.util.DateTimeFormatterUtil;
import ch.sbb.matsim.umlego.util.PathUtil;
import ch.sbb.matsim.umlego.util.RunId;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.opencsv.exceptions.CsvException;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UmlegoConfig {

    private static final Logger LOG = LogManager.getLogger(UmlegoConfig.class);

    private final String zones;
    private final String zoneConnections;
    private final String rawSchedule;
    private final String calendarFileName;
    private final int threads;
    private final String outputFolder;
    private final Set<UmlegoWriterType> writers;
    private final Map<String, String> matrixPaths;
    private final List<LocalDate> dates;
    private final RunId runId;
    private final String simbaRunId;
    private final int year;
    private final boolean isCron;
    private final String inputFolder;

    private UmlegoCalendar calendar = null;

    /**
     * Constructor. Instatiate class with load or create factory method.
     */
    private UmlegoConfig(
            String zones,
            String zoneConnections,
            String rawSchedule,
            String calendarFileName,
            int threads,
            String outputFolder,
            Set<UmlegoWriterType> writers,
            Map<String, String> matrixPaths,
            List<LocalDate> dates,
            RunId runId,
            String simbaRunId,
            int year,
            boolean isCron,
            String inputFolder
    ) {
        this.zones = zones;
        this.zoneConnections = zoneConnections;
        this.rawSchedule = rawSchedule;
        this.calendarFileName = calendarFileName;
        this.threads = threads;
        this.outputFolder = outputFolder;
        this.writers = writers;
        this.matrixPaths = matrixPaths;
        this.dates = dates;
        this.runId = runId;
        this.simbaRunId = simbaRunId;
        this.year = year;
        this.isCron = isCron;
        this.inputFolder = inputFolder;
    }

    public String zones() {
        return this.zones;
    }

    public String zoneConnections() {
        return this.zoneConnections;
    }

    public Set<UmlegoWriterType> writers() {
        return this.writers;
    }

    /**
     * Return calendar object. Calendar is lazy initialized based on file in backup folder.
     *
     * @return UmlegoCalendar
     */
    public UmlegoCalendar calendar() {
        if (this.calendar == null) {
            this.calendar = createCalender(this.calendarFileName);
        }
        return this.calendar;
    }

    public int threads() {
        return this.threads;
    }

    public String outputFolder() {
        return this.outputFolder;
    }

    public String inputFolder() {
        return this.inputFolder;
    }

    public String getHafasFolder() {
        return this.rawSchedule;
    }

    private Map<String, String> matrixPaths() {
        return this.matrixPaths;
    }

    public RunId runId() {
        return this.runId;
    }

    public String simbaRunId() {
        return this.simbaRunId;
    }

    public int year() {
        return this.year;
    }

    public boolean isCron() {
        return this.isCron;
    }

    public List<LocalDate> dates() {
        return this.dates;
    }

    public static boolean isRunningLocally() {
        return true;
        //String runningLocally = System.getProperty("LOCAL");
        //return Boolean.parseBoolean(runningLocally);
    }

    /**
     * Factory methodd returning a mainly static UmlegoConfig based a fixed file structure.
     *
     * @param runId current runId
     * @return UmlegoConfig
     */
    public static UmlegoConfig create(RunId runId, String simbaRunId, int year, boolean isCron, List<LocalDate> targetDates, String inputFolder) {
        String inputBackupFolder = PathUtil.getInputBackupFolder(runId, year) + "/";
        return new UmlegoConfig(
                inputBackupFolder + "zones.csv",
                inputBackupFolder + "anbindungen.csv",
                "/tmp/hafas_fahrplan",
                inputBackupFolder + "calendar.csv",
                8,
                "/tmp",
                Set.of(UmlegoWriterType.BLP, UmlegoWriterType.SKIM, UmlegoWriterType.STATS_JDBC, UmlegoWriterType.STATS_FILE),
                Map.of(),
                targetDates,
                runId,
                simbaRunId,
                year,
                isCron,
                inputFolder);
    }

    public static UmlegoConfig load(String filePath) throws IOException {
        System.setProperty("LOCAL", "true");
        FileSystem fs = getFileSystem();
        InputStream inputStream = fs.open(new Path(getRootDir() + filePath));
        return load(inputStream);
    }

    public static UmlegoConfig load(InputStream inputStream) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        RawConfig rawConfig = mapper.readValue(inputStream, RawConfig.class);

        Set<UmlegoWriterType> writersSet = (rawConfig.writers == null) ? Set.of() :
                rawConfig.writers
                        .stream()
                        .map(String::trim)
                        .map(UmlegoWriterType::valueOf)
                        .collect(Collectors.toSet());

        // Use a default value for runId if it is null
        // TODO discuss fp_version with Abel/Davi (Simbyte: 34, Umlego: 2022)
        // String resolvedSimbaRunId = (rawConfig.simbaRunId == null) ? "00_run" : rawConfig.simbaRunId;

        return new UmlegoConfig(
                rawConfig.zones,
                rawConfig.zoneConnections,
                rawConfig.rawSchedule,
                rawConfig.calendar,
                rawConfig.threads,
                rawConfig.outputFolder,
                writersSet,
                rawConfig.matrixPaths,
                rawConfig.dates.stream().map(d -> LocalDate.parse(d, DateTimeFormatterUtil.DATE_FORMATTER)).toList(),
                null,
                null,
                0,
                false,
                null
        );
    }

    /**
     * Returns path to demand matrices (mapped by baseDemand (saison), i.E. So_w, Sa_w)
     *
     * @param date day for Umlego
     * @return String path to demand matrices directory.
     */
    public String getDemandMatricesPath(LocalDate date) {
        String saison = getCalendarDayRecord(date).baseDemand();
        return matrixPaths().get(saison);
    }

    /**
     * Returns filenames of correction factor matrices.
     *
     * @param date for which we calculate
     * @return String[] array with filenames.
     */
    public String[] getFactorMatricesFileNames(LocalDate date) {
        String path = isRunningLocally() ? "input/factors/" : PathUtil.getInputBackupFolder(runId, year) + "/factors/";
        return getFactorMatricesFileNames(date, path);
    }

    private String[] getFactorMatricesFileNames(LocalDate date, String path) {
        return getCalendarDayRecord(date).correctionFactors().stream()
                .map(s -> path + s + ".mtx")
                .toArray(String[]::new);
    }

    private UmlegoCalendar.UmlegoCalendarDay getCalendarDayRecord(LocalDate date) {
        return calendar().getCalendarDayRecord(date);
    }

    private record RawConfig(
            String zones,
            String zoneConnections,
            String rawSchedule,
            String calendar,
            int threads,
            String outputFolder,
            List<String> writers,
            Map<String, String> matrixPaths,
            List<String> dates
    ) {

    }

    private UmlegoCalendar createCalender(String fileName) {
        try (FileSystem fs = getFileSystem();
             InputStream inputStream = fs.open(new Path(getRootDir() + fileName))) {

            LOG.info("Parsing calender from {}", new Path(getRootDir() + fileName));
            return new UmlegoCalendar(inputStream);
        } catch (IOException | CsvException e) {
            throw new RuntimeException("Error while creating calendar from file " + new Path(getRootDir() + fileName), e);
        }
    }
}
