package ch.sbb.matsim.umlego;

import ch.sbb.matsim.umlego.config.UmlegoConfig;
import ch.sbb.matsim.umlego.util.PathUtil;
import ch.sbb.matsim.umlego.util.RunId;
import com.opencsv.exceptions.CsvException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * The UmlegoLocalMultiDateRunner executes the Umlegung in a purely local context. It's meant to run on a local machine.
 * Configuration is done with config.yaml file.
 *
 * <li>HAFAS Timetable (unzipped) must be available on an accessible filesystem</li>
 * <li>demand matrices must be available as .mtx files on an accessible filesystem</li>
 * <li>correction factor files must be available on an accessible filesystem</li>
 * <li>intput files (zonesLookup, anbindungen, calendar) must be an accessible filesystem</li>
 *
 * <li>output is written as file to output folder configured in config.yaml</li>
 */
public class UmlegoLocalMultiDateRunner extends AbstractMultiDateRunner {

    public static void main(String[] args) throws IOException, CsvException {
        if (args.length < 1) {
            throw new IllegalArgumentException("Please provide the config path.");
        } else if (args.length == 1) {
            UmlegoConfig config = UmlegoConfig.load(args[0]);
            UmlegoLocalMultiDateRunner runner = new UmlegoLocalMultiDateRunner(config);
            runner.init();
            runner.run();
        }
    }

    private void init() {
        System.setProperty("LOCAL", "true");
        printHeapSizes();
    }

    /**
     * Constructor
     *
     * @param config from config.yaml
     */
    public UmlegoLocalMultiDateRunner(UmlegoConfig config) {
        super(config);
    }

    @Override
    protected void checkSaisonRunId(String saisonRunId) {
        // do nothing
    }

    @Override
    boolean isDateAlreadyCalculated(LocalDate targetDate, String timetable) {
        return false;
    }

    @Override
    void backupInputFiles() {
        // do nothing
    }

    @Override
    void prepareOutputFolder(LocalDate date, int index) {
        String outputFolder = getLocalOutputFolder(date, index);
        PathUtil.ensureDir(outputFolder);
    }

    @Override
    String getLocalOutputFolder(LocalDate targetDate, int index) {
        return PathUtil.getLocalOutputFolder(getConfig(), targetDate, index);
    }

    @Override
    void cleanUp(LocalDate date, int index) {
        // do nothing
    }

    @Override
    void writeMetadata(RunId runId) {
        // do nothing
    }

    @Override
    void writeMetadata(RunId runId, LocalDate targetDate) {
        // do nothing
    }

    @Override
    void writeMetadataRuntime(RunId runId, long duration) {
        // do nothing
    }

    @Override
    void writeMetadataRuntime(RunId runId, LocalDate targetDate, long duration) {
        // do nothing
    }

    @Override
    protected boolean isRunIdExisting(RunId runId) {
        return false;
    }
}