package ch.sbb.matsim.umlego.util;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeFormatterUtil {

    public static final String CH_LOCAL_DATE = "dd.MM.yyyy";
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(CH_LOCAL_DATE);
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String ISO_LOCAL_DATE_COMPACT = "yyyyMMdd";
    public static final DateTimeFormatter ISO_FORMATTER_COMPACT = DateTimeFormatter.ofPattern(ISO_LOCAL_DATE_COMPACT);


    private DateTimeFormatterUtil() {}

    public static String format(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }


    public static String format(ZonedDateTime date) {
        return date.format(TIMESTAMP_FORMATTER);
    }

    public static String getAbbreviatedWeekday(LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE");
        return date.format(formatter).toUpperCase();
    }
}
