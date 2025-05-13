package ch.sbb.matsim.umlego.ftp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FTPDownloaderTest {


    @Test
    void testConstructor() {
        FTPDownloader d = new FTPDownloader("/tmp/hafas_fahrplan/2024");

        assertThat(d.getDownloadFolder()).isEqualTo(("/tmp/hafas_fahrplan/2024"));
    }

}