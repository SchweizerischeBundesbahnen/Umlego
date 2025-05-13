package ch.sbb.matsim.umlego.config.credentials;

public class AzureCredentials {

    private AzureCredentials() {}

    public static String getUserName() {
        return System.getenv("AZURE_CLIENT_ID");
    }

    public static String getPassword() {
        return System.getenv("AZURE_CLIENT_SECRET");
    }
}
