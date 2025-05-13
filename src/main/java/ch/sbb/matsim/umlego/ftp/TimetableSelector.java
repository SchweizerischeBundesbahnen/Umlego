package ch.sbb.matsim.umlego.ftp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class TimetableSelector {

    static Logger logger = LogManager.getLogger(TimetableSelector.class);

    private TimetableSelector() {
    }

    /**
     * Selects files from the given list that are not present in the Snowflake `metadata` table, filtering out files
     * that do not belong to the given timetable year. If no file is found or multiple are found it takes the latest
     * timetable file
     *
     * @param fileNames a list of file names to check
     * @param year wished timetable year
     * @return a string of missing file names (concatenated with commas if multiple)
     */
    public static String selectTimetableFileName(List<String> fileNames, int year) {
        if (fileNames == null || fileNames.isEmpty()) {
            throw new RuntimeException("File names list must not be null or empty.");
        }
        List<String> filteredFiles = filterFilesByYear(fileNames, year);
        if (filteredFiles.isEmpty()) {
            throw new RuntimeException("No files matching the specified timetable-year's criteria were found.");
        } else {
            logger.info("Filtered file names: {}", filteredFiles);
        }
        return selectLatestTimetableFile(filteredFiles);
    }

    /**
     * Filters the file names to include only those that match year.
     *
     * @param fileNames the list of file names
     * @param year the wished timetable year
     * @return a list of file names matching the year criteria
     */
    private static List<String> filterFilesByYear(List<String> fileNames, int year) {
        logger.info("Filtering files for the wished timetable year ({}).", year);

        return fileNames.stream()
                .filter(fileName -> {
                    String baseName = FilenameUtils.removeExtension(fileName);
                    String[] parts = baseName.split("_");
                    if (parts.length > 0) {
                        String yearPart = parts[parts.length - 1];
                        try {
                            int fileYear = Integer.parseInt(yearPart);
                            return fileYear == year;
                        } catch (NumberFormatException e) {
                            logger.warn("Invalid year format in file name: {}", fileName);
                        }
                    }
                    return false;
                })
                .toList();
    }



    /**
     * Selects the latest timetable file from the given list of file names.
     *
     * @param filteredFileNames the list of filtered file names
     * @return the latest timetable file name
     */
    private static String selectLatestTimetableFile(List<String> filteredFileNames) {
        return filteredFileNames.stream()
                .max(Comparator.comparing(TimetableSelector::extractTimestamp))
                .orElseThrow(() -> new RuntimeException("Unable to determine the latest timetable file."));
    }

    /**
     * Extracts the timestamp from the file name for comparison.
     *
     * @param fileName the file name to extract the timestamp from
     * @return the parsed timestamp as a `LocalDateTime`
     */
    private static LocalDateTime extractTimestamp(String fileName) {
        try {
            String baseName = FilenameUtils.removeExtension(fileName);
            String[] parts = baseName.split("_");
            if (parts.length < 2) {
                throw new RuntimeException("Invalid file name format: " + fileName);
            }
            String datePart = parts[0];
            String timePart = parts[1];
            return LocalDateTime.parse(datePart + "T" + timePart, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract timestamp from file name: " + fileName, e);
        }
    }
}

