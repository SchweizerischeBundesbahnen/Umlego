package ch.sbb.matsim.umlego.util;

import ch.sbb.matsim.umlego.metadata.MetadataKey;
import ch.sbb.matsim.umlego.metadata.MetadataRepository;
import java.io.IOException;
import java.sql.Connection;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for copying files and directories and inserting their metadata into a repository.
 *
 * This class provides static methods to perform following operations:
 * - copying files
 * - copying directories
 * - inserting metadata
 */
public final class MetadataUtil {

    private static Logger LOG = LogManager.getLogger(MetadataUtil.class);

    private MetadataUtil() {}

    public static void setLogger(Logger logger) {
        LOG = logger;
    }

    /**
     * Inserts metadata for a file into the repository.
     *
     * @param fs the Hadoop FileSystem to use
     * @param srcFilePath the source file path
     * @param repo the metadata repository to insert into
     * @param connection the database connection to use
     * @param runIdValue the run identifier value
     * @param key the metadata key
     * @throws IOException if an I/O error occurs during file operations
     */
    public static void insertFileMetadata(FileSystem fs, String srcFilePath, MetadataRepository repo,
            Connection connection, String runIdValue, MetadataKey key) throws IOException {
        Path srcPath = new Path(srcFilePath);

        try {
            FileStatus fileStatus = fs.getFileStatus(srcPath);
            String modificationTime = formatModificationTime(fileStatus.getModificationTime());

            repo.insertMetadata(connection, runIdValue, key, modificationTime);
            LOG.info("Inserted metadata for key {} with modification time {}", key, modificationTime);
        } catch (IOException e) {
            LOG.error("Error inserting metadata for file {}", srcPath, e);
            throw e;
        }
    }

    /**
     * Inserts metadata for a directory into the repository.
     *
     * @param fs the Hadoop FileSystem to use
     * @param srcDirPath the source directory path
     * @param repo the metadata repository to insert into
     * @param connection the database connection to use
     * @param runIdValue the run identifier value
     * @param key the metadata key
     * @throws IOException if an I/O error occurs during file operations
     */
    public static void insertDirectoryMetadata(FileSystem fs, String srcDirPath, MetadataRepository repo,
            Connection connection, String runIdValue, MetadataKey key) throws IOException {
        Path srcPath = new Path(srcDirPath);

        try {
            String latestModificationTime = getLatestModificationTime(fs, srcPath);
            repo.insertMetadata(connection, runIdValue, key, latestModificationTime);
            LOG.info("Inserted metadata for key {} with latest modification time {}", key, latestModificationTime);
        } catch (IOException e) {
            LOG.error("Error inserting metadata for directory {}", srcPath, e);
            throw e;
        }
    }

    /**
     * Retrieves the latest modification time among all files within a directory.
     *
     * @param fs the Hadoop FileSystem to use
     * @param directoryPath the path of the directory to scan
     * @return the latest modification time in milliseconds since the epoch
     * @throws IOException if an I/O error occurs while accessing the file system
     */
    private static String getLatestModificationTime(FileSystem fs, Path directoryPath) throws IOException {
        long latestModificationTime = 0;
        RemoteIterator<LocatedFileStatus> fileIterator = fs.listFiles(directoryPath, true);

        while (fileIterator.hasNext()) {
            LocatedFileStatus fileStatus = fileIterator.next();
            long modificationTime = fileStatus.getModificationTime();
            if (modificationTime > latestModificationTime) {
                latestModificationTime = modificationTime;
            }
        }
        return formatModificationTime(latestModificationTime);
    }

    private static String formatModificationTime(long modificationTime) {
        Instant instant = Instant.ofEpochMilli(modificationTime);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }
}
