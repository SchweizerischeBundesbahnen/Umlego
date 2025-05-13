package ch.sbb.matsim.umlego.ftp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public final class UnzipUtil {

    private static final Logger LOG = LogManager.getLogger(UnzipUtil.class);

    private UnzipUtil() {
    }

    /**
     * Unzips a specified ZIP file to a target directory.
     *
     * @param zipFile the ZIP file to unzip
     * @param destDir the destination directory for extracted files
     * @throws IOException if an I/O error occurs during unzipping
     */
    public static void unzipFile(File zipFile, String destDir) throws IOException {
        LOG.info("Starting to unzip file '{}' to directory '{}'", zipFile.getAbsolutePath(), destDir);
        Files.createDirectories(Paths.get(destDir));
        int fileCount = 0;
        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(zipFile.toPath()))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                File filePath = new File(destDir, entry.getName());
                if (!entry.isDirectory()) {
                    try (FileOutputStream fos = new FileOutputStream(filePath)) {
                        byte[] bytesIn = new byte[4096];
                        int read;
                        while ((read = zipIn.read(bytesIn)) != -1) {
                            fos.write(bytesIn, 0, read);
                        }
                    }
                    fileCount++;
                    LOG.info("Extracted file: '{}'", filePath.getAbsolutePath());
                } else {
                    Files.createDirectories(filePath.toPath());
                    LOG.info("Created directory: '{}'", filePath.getAbsolutePath());
                }
                zipIn.closeEntry();
            }
        } catch (IOException e) {
            LOG.error("Error while unzipping file '{}': {}", zipFile.getAbsolutePath(), e.getMessage());
            throw e;
        }
        LOG.info("Unzipping completed successfully. {} files extracted to '{}'", fileCount, destDir);
    }
}
