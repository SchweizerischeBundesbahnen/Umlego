package ch.sbb.matsim.umlego.config;

import ch.sbb.matsim.umlego.util.DateTimeFormatterUtil;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The UmlegoCalendar stores information on which Demand and which Factors are to be used for any given date.
 * The {@link UmlegoCalendarDay} class represents a single day in the calendar.

 */
public class UmlegoCalendar {

    private final Map<LocalDate, UmlegoCalendarDay> calendarDays;

    public UmlegoCalendar(String filePath) throws IOException, CsvException {
        this.calendarDays = parseCalendar(filePath);
    }

    public UmlegoCalendar(InputStream inputStream) throws IOException, CsvException {
        this.calendarDays = parseCalendar(inputStream);
    }

    public UmlegoCalendarDay getCalendarDayRecord(LocalDate date) {
        return calendarDays.get(date);
    }

    public Set<LocalDate> getCalendarDays() {
        return calendarDays.keySet();
    }

    /**
     * Checks if target date is in current calendar.
     *
     * @param targetDate date to check
     * @return boolean false if not in calendar.
     */
    public boolean isDateInCalendar(LocalDate targetDate) {
        return getCalendarDays().contains(targetDate);
    }

    /**
     * A record that represents a single day in the {@link UmlegoCalendar}.
     * It contains the weekday (1-7), the base demand, and the correction factors.
     */
    public record UmlegoCalendarDay(
        LocalDate date,
        int weekday,
        String baseDemand,
        List<String> correctionFactors
    ) {}

    /**
     * Parse a CSV file and return a map from dates to {@link UmlegoCalendarDay} objects.
     * The CSV file should have the following structure:
     * <ul>
     *     <li>The first column should contain the dates in the format as specified in the constructor.
     *     <li>The second column should contain the day of the week (1-7).
     *     <li>The third column should contain the base demand.
     *     <li>The remaining columns should contain the correction factors.
     * </ul>
     * The method will skip the first row (header row) and then parse the remaining rows.
     * The returned map will contain a mapping from each date to the corresponding {@link UmlegoCalendarDay} object.
     *
     * @param filePath the path to the CSV file
     * @return the map from dates to {@link UmlegoCalendarDay} objects
     * @throws IOException if there is an error reading the file
     * @throws CsvException if there is an error parsing the CSV
     */
    public Map<LocalDate, UmlegoCalendarDay> parseCalendar(String filePath) throws IOException, CsvException {
        Reader fileReader = new FileReader(filePath);
        return parseCalendar(fileReader);
    }

    public Map<LocalDate, UmlegoCalendarDay> parseCalendar(InputStream inputStream) throws IOException, CsvException {
        Reader inputStreamReader = new InputStreamReader(inputStream);
        return parseCalendar(inputStreamReader);
    }


    public Map<LocalDate, UmlegoCalendarDay> parseCalendar(Reader reader) throws IOException, CsvException {
        try (CSVReader csvReader = new CSVReader(reader)) {
            List<String[]> lines = csvReader.readAll();
            String[] headers = lines.getFirst();

            return lines.stream()
                    .skip(1) // Skip header row
                    .map(line -> {
                        LocalDate date = LocalDate.parse(line[0], DateTimeFormatterUtil.DATE_FORMATTER);
                        int weekday = Integer.parseInt(line[1].trim());
                        String baseDemand = line[2].trim();

                        List<String> correctionFactors = new ArrayList<>();
                        for (int i = 3; i < headers.length; i++) {
                            if (line[i] != null && !line[i].isBlank()) {
                                correctionFactors.add(line[i].trim());
                            }
                        }

                        return new UmlegoCalendarDay(date, weekday, baseDemand, correctionFactors);
                    })
                    .collect(Collectors.toMap(UmlegoCalendarDay::date, Function.identity()));
        }
    }
}
