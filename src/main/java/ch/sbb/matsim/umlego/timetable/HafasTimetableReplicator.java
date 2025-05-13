package ch.sbb.matsim.umlego.timetable;

import ch.sbb.matsim.umlego.config.hadoop.AzureFileSystemConfiguration;
import ch.sbb.matsim.umlego.config.hadoop.LocalFileSystemConfiguration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Class replicates HAFAs Fahrplan files from Azure Blob Storage to OpenShift local file context.
 *
 * Used for local development.
 *
 * In production environment, HAFAS Fahrplan files come from FTP-Server.
 */
public class HafasTimetableReplicator {

    private static final Logger LOG = LogManager.getLogger(HafasTimetableReplicator.class);

    public static void main(String[] args) {
        HafasTimetableReplicator me = new HafasTimetableReplicator();
        me.replicate("Input_Fahrplan/2024-09-17_100640_001_SBB_Rohdaten_2024");
    }

    public void replicate(String outputFolder) {
        try {
            LOG.info("Replicating files from Azure to local context [" + outputFolder + "]");
            FileSystem fs = FileSystem.get(AzureFileSystemConfiguration.get());
            FileSystem.getLocal(LocalFileSystemConfiguration.create()).setWriteChecksum(false);

            fs.copyToLocalFile(new Path("/simba_umlego/fahrplan/ECKDATEN"), new Path(outputFolder + "/ECKDATEN"));
            fs.copyToLocalFile(new Path("/simba_umlego/fahrplan/BFKOORD_WGS"), new Path(outputFolder + "/BFKOORD_WGS"));
            fs.copyToLocalFile(new Path("/simba_umlego/fahrplan/BITFELD"), new Path(outputFolder + "/BITFELD"));
            fs.copyToLocalFile(new Path("/simba_umlego/fahrplan/BETRIEB_DE"), new Path(outputFolder + "/BETRIEB_DE"));
            fs.copyToLocalFile(new Path("/simba_umlego/fahrplan/FPLAN"), new Path(outputFolder + "/FPLAN"));

            LOG.info("Replication finished");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
