package ch.sbb.matsim.umlego.config.credentials;

public class FtpCredentials {

    private FtpCredentials() {}

    public static String getUserName() {
        return System.getenv("INFOP-FTP-USER");
    }

    public static String getPassword() {
        return System.getenv("INFOP-FTP-PASSWORD");
    }

    public static String getHostname() {
        return System.getenv("INFOP-FTP-SERVER");
    }

}
