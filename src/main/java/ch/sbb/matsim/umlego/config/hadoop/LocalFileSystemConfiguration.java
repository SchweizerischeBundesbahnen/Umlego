package ch.sbb.matsim.umlego.config.hadoop;

import org.apache.hadoop.conf.Configuration;

import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;

public class LocalFileSystemConfiguration {

    public static Configuration create() {
        Configuration conf = new Configuration();
        conf.set(FS_DEFAULT_NAME_KEY, "file:///");
        return conf;
    }

}
