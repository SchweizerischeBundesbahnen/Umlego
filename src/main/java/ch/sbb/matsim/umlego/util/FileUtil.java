package ch.sbb.matsim.umlego.util;

import java.io.IOException;

import ch.sbb.matsim.umlego.config.hadoop.FileSystemUtil;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for copying files and directories and inserting their metadata into a repository.
 *
 * This class provides static methods to perform following operations:
 * - copying files
 * - copying directories
 */
public final class FileUtil {

    private static final Logger LOG = LogManager.getLogger(FileUtil.class);

    private FileUtil() {}

    /**
     * Copies a file to the destination folder.
     *
     * @param srcFilePath the source file path
     * @param dstFolderPath the destination folder path
     * @throws IOException if an I/O error occurs during file operations
     */
    public static void copyFile(String srcFilePath, String dstFolderPath) throws IOException {
        FileSystem fs = FileSystemUtil.getFileSystem();
        Path srcPath = new Path(srcFilePath);
        Path dstPath = new Path(dstFolderPath);

        try {
            org.apache.hadoop.fs.FileUtil.copy(fs, srcPath, fs, dstPath, false, fs.getConf());
            LOG.info("Copied file from {} to {}", srcPath, dstPath);
        } catch (IOException e) {
            LOG.error("Error copying file from {} to {}", srcPath, dstPath, e);
            throw e;
        }
    }

    /**
     * Copies a directory to the destination folder.
     *
     * @param srcDirPath the source directory path
     * @param dstFolderPath the destination folder path
     * @throws IOException if an I/O error occurs during file operations
     */
    public static void copyDirectory(String srcDirPath, String dstFolderPath) throws IOException {
        FileSystem fs = FileSystemUtil.getFileSystem();
        Path srcPath = new Path(srcDirPath);
        Path dstPath = new Path(dstFolderPath);

        try {
            org.apache.hadoop.fs.FileUtil.copy(fs, srcPath, fs, dstPath, false, fs.getConf());
            LOG.info("Copied directory from {} to {}", srcPath, dstPath);
        } catch (IOException e) {
            LOG.error("Error copying directory from {} to {}", srcPath, dstPath, e);
            throw e;
        }
    }

    /**
     * Creates a directory on configured FileSystem.
     *
     * @param dirPath
     * @throws IOException
     */
    public static boolean createDirectory(String dirPath) throws IOException {
        return FileSystemUtil.getFileSystem().mkdirs(new Path(dirPath));
    }
}
