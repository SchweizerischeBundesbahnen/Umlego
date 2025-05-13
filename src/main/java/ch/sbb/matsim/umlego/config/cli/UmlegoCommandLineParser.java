package ch.sbb.matsim.umlego.config.cli;

import ch.sbb.matsim.umlego.config.ExitCode;
import ch.sbb.matsim.umlego.config.UmlegoException;
import ch.sbb.matsim.umlego.util.RunId;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import static java.lang.String.format;

public class UmlegoCommandLineParser {

    private CommandLine cmdLine = null;

    public void parse(String[] args) {
        Options options = new Options();

        Option runOption = new Option("r", "run", true, "run-id when possible in the format yyyymmdd");
        runOption.setRequired(false);
        options.addOption(runOption);

        Option simbaRunOption = new Option("s", "saison_run_base", true, "saison-run-id (only the base part, format fp_yymmdd");
        simbaRunOption.setRequired(false);
        options.addOption(simbaRunOption);

        Option timetableYearOption = new Option("y", "year", true, "timetable year to umlego format yyyy");
        timetableYearOption.setRequired(true);
        options.addOption(timetableYearOption);

        Option cronOption = new Option("c", "cron", true, "is started by a cronjob");
        cronOption.setRequired(false);
        options.addOption(cronOption);

        Option targetDatesOption = new Option("d", "target_dates", true, "comma-separated list of target dates, format yyyymmdd or relative to today (-1, 0, 1, etc.)");
        targetDatesOption.setRequired(true);
        options.addOption(targetDatesOption);

        Option inputFolderOption = new Option("if", "input_folder", true, "input folder for config files, relative to input folder of current year");
        inputFolderOption.setRequired(false);
        options.addOption(inputFolderOption);

        CommandLineParser parser = new DefaultParser();

        try {
            cmdLine = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            new HelpFormatter().printHelp("UmlegoMultiDateRunner", options);
            throw new UmlegoException("Parsing Exception", e, ExitCode.PARSING_ERROR);
        }
    }

    public RunId getRunId() {
        String runId = cmdLine.getOptionValue('r');
        return (StringUtils.isNotEmpty(runId)) ? new RunId(runId) : generateRunId();
    }

    private RunId generateRunId() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
        String dateTime = LocalDateTime.now().format(formatter);
        return new RunId(format("%s_%s", dateTime, "artif"));
    }

    public String getSimbaRunId() {
        return cmdLine.getOptionValue("s");
    }

    public List<LocalDate> getTargetDates() {
        TargetDatesParser parser = new TargetDatesParser();
        return parser.parse(cmdLine.getOptionValue("d"));
    }

    public int getYear() {
        return Integer.parseInt(cmdLine.getOptionValue("y"));
    }

    public boolean isCron() {
        return Boolean.parseBoolean(cmdLine.getOptionValue("c"));
    }

    public String getInputFolder() {
        String inputFolder = cmdLine.getOptionValue("input_folder");
        return (StringUtils.isNotEmpty(inputFolder)) ? inputFolder : null;
    }

}
