package ch.sbb.matsim.umlego.ftp;

import ch.sbb.matsim.umlego.config.credentials.FtpCredentials;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;


public class FTPDownloader {

    private static final Logger LOG = LogManager.getLogger(FTPDownloader.class);

    private static final String SFTP_DIRECTORY = "Rohdaten_TPSI";
    private ChannelSftp sftpChannel;
    private String downloadFolder;


    public FTPDownloader(String downloadFolder) {
        this.downloadFolder = downloadFolder;
    }

    public String getDownloadDir() {
        return downloadFolder;
    }

    public String getDownloadFolder() {
        return downloadFolder;
    }

    /**
     * Connects to the SFTP server using credentials and configures the SFTP client.
     *
     * @throws Exception if an error occurs during connection
     */
    public void connectToSftpServer() throws Exception {
        LOG.info("Attempting to connect to the SFTP server...");
        JSch jsch = new JSch();
        Session session = jsch.getSession(FtpCredentials.getUserName(), FtpCredentials.getHostname());
        session.setPassword(FtpCredentials.getPassword());

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.connect();

        sftpChannel = (ChannelSftp) session.openChannel("sftp");
        sftpChannel.connect();
        LOG.info("Connected to SFTP server...");
    }

    /**
     * Lists all files in the specified directory on the SFTP server.
     *
     * @return fileNames a list of file names in the server directory
     * @throws SftpException if an error occurs during file listing
     */
    public List<String> listFilesOnServer() throws SftpException {
        LOG.info("Listing files on the SFTP server...");
        sftpChannel.cd(SFTP_DIRECTORY);
        List<String> fileNames = new ArrayList<>();
        Vector<ChannelSftp.LsEntry> files = sftpChannel.ls(".");
        for (ChannelSftp.LsEntry entry : files) {
            if (!entry.getAttrs().isDir()) {
                fileNames.add(entry.getFilename());
            }
        }
        LOG.info("Files listed in directory '{}': {}", SFTP_DIRECTORY, fileNames);
        return fileNames;
    }

    /**
     * Downloads a specified file from the SFTP server to a temporary directory.
     *
     * @param fileName the name of the file to download
     * @return downloadedFile the downloaded file in the temporary directory
     * @throws SftpException if an error occurs during the download
     * @throws IOException if an I/O error occurs when writing the file locally
     */
    public File downloadFile(String fileName) throws SftpException, IOException {
        LOG.info("Downloading: '{}'...", fileName);
        File downloadDir = new File(getDownloadFolder());
        if (!downloadDir.exists()) {
            boolean created = downloadDir.mkdirs();
            if (created) {
                LOG.info("Created download directory: {}", getDownloadFolder());
            } else {
                LOG.warn("Failed to create download directory: {}", getDownloadFolder());
            }
        }
        File tmpDownload = new File(getDownloadFolder(), "hafas_fahrplan.zip");
        try (InputStream inputStream = sftpChannel.get(fileName);
            FileOutputStream outputStream = new FileOutputStream(tmpDownload)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            LOG.info("Downloaded file '{}' and saved as '{}'", fileName, tmpDownload.getAbsolutePath());
        }
        return tmpDownload;
    }

    /**
     * Disconnects from the SFTP server.
     */
    public void disconnect() {
        LOG.info("Disconnecting from the SFTP server...");
        if (sftpChannel != null && sftpChannel.isConnected()) {
            sftpChannel.disconnect();
            try {
                sftpChannel.getSession().disconnect();
                LOG.info("Disconnected from SFTP server.");
            } catch (Exception e) {
                LOG.error("Error while disconnecting from the SFTP server.", e);
            }
        }
    }
}

