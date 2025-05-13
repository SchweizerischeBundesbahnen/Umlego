package ch.sbb.matsim.umlego.config.cli;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TargetDatesParserTest {


    @Test
    void testParseDateDeltas() {
        // Given
        LocalDate today = LocalDate.now();

        // When
        TargetDatesParser parser = new TargetDatesParser();
        List<LocalDate> targeDates = parser.parse("-1, 0, 1, 2, 3, 10");

        // Then
        assertThat(targeDates).hasSize(6);
        assertThat(targeDates.get(0)).isEqualTo(today.minusDays(1));
        assertThat(targeDates.get(1)).isEqualTo(today);
        assertThat(targeDates.get(2)).isEqualTo(today.plusDays(1));
        assertThat(targeDates.get(3)).isEqualTo(today.plusDays(2));
        assertThat(targeDates.get(4)).isEqualTo(today.plusDays(3));
        assertThat(targeDates.get(5)).isEqualTo(today.plusDays(10));
    }

    @Test
    void testParseDateToday() {
        // Given
        LocalDate expectedDate = LocalDate.of(2024, 12, 16);

        // When
        TargetDatesParser parser = new TargetDatesParser();
        List<LocalDate> targeDates = parser.parse("20241216");

        // Then
        assertThat(targeDates).hasSize(1);
        assertThat(targeDates.getFirst()).isEqualTo(expectedDate);
    }

    @Test
    void testParseDateYesterdayTodayTomorrow() {
        // Given
        LocalDate expectedDate = LocalDate.of(2024, 12, 16);

        // When
        TargetDatesParser parser = new TargetDatesParser();
        List<LocalDate> targeDates = parser.parse("20241215, 20241216, 20241217");

        // Then
        assertThat(targeDates).hasSize(3);
        assertThat(targeDates.getFirst()).isEqualTo(expectedDate.minusDays(1));
        assertThat(targeDates.get(1)).isEqualTo(expectedDate);
        assertThat(targeDates.get(2)).isEqualTo(expectedDate.plusDays(1));
    }

    @Test
    void testParseDateInvalidFormat() {
        // Given
        String invalidTargetDates = "20241215, , ,";

        // When
        TargetDatesParser parser = new TargetDatesParser();

        // Then
        InvalidTargetDatesException exception = assertThrows(InvalidTargetDatesException.class, () -> parser.parse(invalidTargetDates));
        assertThat(exception.getMessage()).isEqualTo(format("Invalid format for targetDates [%s]", invalidTargetDates));
    }


}