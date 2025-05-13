package ch.sbb.matsim.umlego.config.hadoop;

import ch.sbb.matsim.umlego.config.credentials.AzureCredentials;
import org.apache.hadoop.conf.Configuration;

import static ch.sbb.matsim.umlego.config.EnvironmentUtil.getStage;
import static java.lang.String.format;
import static org.apache.hadoop.fs.CommonConfigurationKeysPublic.FS_DEFAULT_NAME_KEY;
import static org.apache.hadoop.fs.azurebfs.constants.ConfigurationKeys.*;

public class AzureFileSystemConfiguration {

    private static final String TENANTID = "2cda5d11-f0ac-46b3-967d-af1b2e1bd01a";
    private static final String ACCOUNTNAME = "simbaeapsa";
    private static final String CONTAINERNAME = format("%s-std-simba-dbstagein", getStage());
    private static final String HDFSURI = format("abfs://%s@%s.dfs.core.windows.net/", CONTAINERNAME, ACCOUNTNAME);

    private static Configuration CACHE = create();

    private AzureFileSystemConfiguration() {}

    public static Configuration create() {
        Configuration conf = new Configuration();
        conf.set(FS_DEFAULT_NAME_KEY, HDFSURI);
        conf.set(FS_AZURE_ACCOUNT_AUTH_TYPE_PROPERTY_NAME, "OAuth");
        conf.set(FS_AZURE_ACCOUNT_OAUTH_CLIENT_ENDPOINT, format("https://login.microsoftonline.com/%s/oauth2/token", TENANTID));
        conf.set(FS_AZURE_ACCOUNT_TOKEN_PROVIDER_TYPE_PROPERTY_NAME, "org.apache.hadoop.fs.azurebfs.oauth2.ClientCredsTokenProvider");
        conf.set(FS_AZURE_ACCOUNT_OAUTH_CLIENT_ID, AzureCredentials.getUserName());
        conf.set(FS_AZURE_ACCOUNT_OAUTH_CLIENT_SECRET, AzureCredentials.getPassword());
        return conf;
    }

    public static Configuration get() {
        return CACHE;
    }
}
