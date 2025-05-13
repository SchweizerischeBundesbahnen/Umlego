package ch.sbb.matsim.umlego.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.sbb.matsim.umlego.metadata.MetadataKey;
import ch.sbb.matsim.umlego.metadata.MetadataRepository;
import java.io.IOException;
import java.sql.Connection;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class MetadataUtilTest {

    private FileSystem mockFileSystem;
    private MetadataRepository mockRepository;
    private Connection mockConnection;
    private Logger mockLogger;

    @BeforeEach
    public void setUp() {
        mockFileSystem = mock(FileSystem.class);
        mockRepository = mock(MetadataRepository.class);
        mockConnection = mock(Connection.class);
        mockLogger = mock(Logger.class);
        MetadataUtil.setLogger(mockLogger);
    }

    @Test
    public void testInsertFileMetadata_Success() throws IOException {
        String srcFilePath = "/source/file.txt";
        MetadataKey key = MetadataKey.TIMETABLE;
        String runIdValue = "run1";

        long mockTime = 1701551856000L; // 2023-12-02 22:17:36

        FileStatus mockFileStatus = mock(FileStatus.class);
        when(mockFileSystem.getFileStatus(any(Path.class))).thenReturn(mockFileStatus);
        when(mockFileStatus.getModificationTime()).thenReturn(mockTime);

        try (MockedStatic<LogManager> mockedLogManager = mockStatic(LogManager.class)) {
            mockedLogManager.when(() -> LogManager.getLogger(MetadataUtil.class)).thenReturn(mockLogger);

            MetadataUtil.insertFileMetadata(mockFileSystem, srcFilePath, mockRepository, mockConnection, runIdValue, key);

            verify(mockFileSystem).getFileStatus(eq(new Path(srcFilePath)));
            verify(mockRepository).insertMetadata(eq(mockConnection), eq(runIdValue), eq(key), eq("2023-12-02 22:17:36"));
            verify(mockLogger).info("Inserted metadata for key {} with modification time {}", key, "2023-12-02 22:17:36");
        }
    }

    @Test
    public void testInsertFileMetadata_Failure() throws IOException {
        String srcFilePath = "/source/file.txt";
        MetadataKey key = MetadataKey.TIMETABLE;
        String runIdValue = "run1";

        when(mockFileSystem.getFileStatus(any(Path.class))).thenThrow(new IOException("Test exception"));

        try (MockedStatic<LogManager> mockedLogManager = mockStatic(LogManager.class)) {
            mockedLogManager.when(() -> LogManager.getLogger(MetadataUtil.class)).thenReturn(mockLogger);

            assertThrows(IOException.class, () -> MetadataUtil.insertFileMetadata(
                    mockFileSystem, srcFilePath, mockRepository, mockConnection, runIdValue, key
            ));

            verify(mockFileSystem).getFileStatus(eq(new Path(srcFilePath)));
            verify(mockLogger).error(eq("Error inserting metadata for file {}"), eq(new Path(srcFilePath)), any(IOException.class));
        }
    }

    @Test
    public void testInsertDirectoryMetadata_Success() throws IOException {
        String srcDirPath = "/source/directory";
        MetadataKey key = MetadataKey.RUNTIME;
        String runIdValue = "run1";

        long mockTime1 = 1701551856000L; // 2023-12-02 22:17:36
        long mockTime2 = 1702651856000L; // 2023-12-15 15:50:56

        RemoteIterator<LocatedFileStatus> mockFileIterator = mock(RemoteIterator.class);
        LocatedFileStatus mockFileStatus1 = mock(LocatedFileStatus.class);
        LocatedFileStatus mockFileStatus2 = mock(LocatedFileStatus.class);

        when(mockFileSystem.listFiles(any(Path.class), eq(true))).thenReturn(mockFileIterator);
        when(mockFileIterator.hasNext()).thenReturn(true, true, false);
        when(mockFileIterator.next()).thenReturn(mockFileStatus1, mockFileStatus2);
        when(mockFileStatus1.getModificationTime()).thenReturn(mockTime1);
        when(mockFileStatus2.getModificationTime()).thenReturn(mockTime2);

        try (MockedStatic<LogManager> mockedLogManager = mockStatic(LogManager.class)) {
            mockedLogManager.when(() -> LogManager.getLogger(MetadataUtil.class)).thenReturn(mockLogger);

            MetadataUtil.insertDirectoryMetadata(mockFileSystem, srcDirPath, mockRepository, mockConnection, runIdValue, key);

            verify(mockFileSystem).listFiles(eq(new Path(srcDirPath)), eq(true));
            verify(mockRepository).insertMetadata(eq(mockConnection), eq(runIdValue), eq(key), eq("2023-12-15 15:50:56"));
            verify(mockLogger).info("Inserted metadata for key {} with latest modification time {}", key, "2023-12-15 15:50:56");
        }
    }

    @Test
    public void testInsertDirectoryMetadata_Failure() throws IOException {
        String srcDirPath = "/source/directory";
        MetadataKey key = MetadataKey.RUNTIME;
        String runIdValue = "run1";

        when(mockFileSystem.listFiles(any(Path.class), eq(true))).thenThrow(new IOException("Test exception"));

        try (MockedStatic<LogManager> mockedLogManager = mockStatic(LogManager.class)) {
            mockedLogManager.when(() -> LogManager.getLogger(MetadataUtil.class)).thenReturn(mockLogger);

            assertThrows(IOException.class, () -> MetadataUtil.insertDirectoryMetadata(
                mockFileSystem, srcDirPath, mockRepository, mockConnection, runIdValue, key
            ));

            verify(mockFileSystem).listFiles(eq(new Path(srcDirPath)), eq(true));
            verify(mockLogger).error(eq("Error inserting metadata for directory {}"), eq(new Path(srcDirPath)), any(IOException.class));
        }
    }
}
