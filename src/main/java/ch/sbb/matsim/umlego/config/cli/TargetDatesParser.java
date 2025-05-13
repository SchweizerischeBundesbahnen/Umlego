package ch.sbb.matsim.umlego.config.cli;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

/**
 * The TargetDatesParser parse two possible formats which can be a comma-separated list of either:
 *  <li>Absolute dates, in the format yyyymmdd, or</li>
 *  <li>Date-deltas relative to "today", e.g. -1, 0, 1, 2, 3, 10</li>
 */
public class TargetDatesParser {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * Parses content of target_dates (absolute or relative-deltas) and
     * returns a list of local dates.
     *
     * @param targetDates dates for which an Umlegung should run
     * @return List<LocalDate>
     */
    public List<LocalDate> parse(String targetDates) {
        try {
            return parseAbsoluteDates(targetDates);
        } catch (Exception e1) {
            try {
                return parseDeltaDates(targetDates);
            } catch (Exception e2) {
                throw new InvalidTargetDatesException(format("Invalid format for targetDates [%s]", targetDates), e2);
            }
        }
    }

    private List<LocalDate> parseAbsoluteDates(String targetDates) {
        String[] dateStrings = targetDates.split(",");
        return Arrays.stream(dateStrings).map(date -> parseDate(date.trim())).toList();
    }

    private LocalDate parseDate(String date) {
        return LocalDate.parse(date, DATE_FORMAT);
    }

    private List<LocalDate> parseDeltaDates(String targetDates) {
        String[] deltaStrings = targetDates.split(",");
        return Arrays.stream(deltaStrings).map(delta -> LocalDate.now().plusDays(Long.parseLong(delta.trim()))).toList();
    }
}
