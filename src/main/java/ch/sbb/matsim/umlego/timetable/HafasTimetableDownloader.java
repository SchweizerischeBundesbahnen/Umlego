package ch.sbb.matsim.umlego.timetable;

import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.closeConnection;
import static ch.sbb.matsim.umlego.config.snowflake.SnowflakeConfig.connect;
import static ch.sbb.matsim.umlego.ftp.UnzipUtil.unzipFile;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.SAISON_BASE_ID;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.TIMETABLE;
import static ch.sbb.matsim.umlego.metadata.MetadataKey.YEAR;

import ch.sbb.matsim.umlego.config.NoRunNeededException;
import ch.sbb.matsim.umlego.ftp.FTPDownloader;
import ch.sbb.matsim.umlego.ftp.TimetableSelector;
import ch.sbb.matsim.umlego.metadata.MetadataRepository;
import ch.sbb.matsim.umlego.util.RunId;
import java.io.File;
import java.sql.Connection;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The HafasTimetableDownloader manages access to INFO+ sFTP server and prepares timetable files (naming) as expected by
 * the HafasConverter.
 */
public class HafasTimetableDownloader {

    private static final Logger LOG = LogManager.getLogger(HafasTimetableDownloader.class);

    public String downloadAndExtractFile(String downloadFolder, RunId runId, int year, boolean isCron, String saisonBaseId) {
        try {
            String timetableFile = null;
            FTPDownloader ftpDownloader = new FTPDownloader(downloadFolder);
            ftpDownloader.connectToSftpServer();
            List<String> files = ftpDownloader.listFilesOnServer();
            if (!files.isEmpty()) {
                timetableFile = TimetableSelector.selectTimetableFileName(files, year);

                Connection connection = connect();
                MetadataRepository repo = new MetadataRepository();

                if (isCron) {
                    boolean shouldRunCron = repo.shouldRunCron(connection, timetableFile, saisonBaseId);
                    if (!shouldRunCron) {
                        throw new NoRunNeededException(
                                "There is already a Run with the latest timetable version and the given Saison-Base-ID");
                    }
                }
                repo.insertMetadata(connection, runId.getValue(), TIMETABLE, timetableFile);
                repo.insertMetadata(connection, runId.getValue(), YEAR, year);
                repo.insertMetadata(connection, runId.getValue(), SAISON_BASE_ID, saisonBaseId);

                File downloadedFile = ftpDownloader.downloadFile(timetableFile);
                unzipFile(downloadedFile, ftpDownloader.getDownloadFolder());
                closeConnection(connection);
            } else {
                LOG.warn("No files found on the SFTP server.");
            }
            ftpDownloader.disconnect();
            return timetableFile;
        } catch (Exception e) {
            LOG.error("An error occurred during the SFTP file download and extraction process.", e);
            return null;
        }
    }
}
