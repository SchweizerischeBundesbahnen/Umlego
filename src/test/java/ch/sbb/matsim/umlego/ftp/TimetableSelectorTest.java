package ch.sbb.matsim.umlego.ftp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

public class TimetableSelectorTest {

    @Test
    public void testSelectTimetableFileName_SelectLatestOfGivenYear() {
        List<String> fileNames = List.of(
                "2024-11-21_154759_001_SBB_Rohdaten_2024.zip",
                "2024-11-19_154759_001_SBB_Rohdaten_2024.zip",
                "2024-11-19_154759_001_SBB_Rohdaten_2025.zip"
        );
        String result = TimetableSelector.selectTimetableFileName(fileNames, 2024);
        assertEquals("2024-11-21_154759_001_SBB_Rohdaten_2024.zip", result);
    }

    @Test
    public void testSelectTimetableFileName_NoMatchingYear() {
        List<String> fileNames = List.of(
                "2023-11-21_154759_001_SBB_Rohdaten_2023.zip",
                "2023-11-19_154759_001_SBB_Rohdaten_2023.zip"
        );
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> TimetableSelector.selectTimetableFileName(fileNames, 2024)
        );
        assertEquals("No files matching the specified timetable-year's criteria were found.", exception.getMessage());
    }

    @Test
    public void testSelectTimetableFileName_EmptyFileNameNames() {
        List<String> fileNames = List.of();
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> TimetableSelector.selectTimetableFileName(fileNames, 2024)
        );
        assertEquals("File names list must not be null or empty.", exception.getMessage());
    }

    @Test
    public void testSelectTimetableFile_NullFileNameNames() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> TimetableSelector.selectTimetableFileName(null, 2024)
        );
        assertEquals("File names list must not be null or empty.", exception.getMessage());
    }
}
