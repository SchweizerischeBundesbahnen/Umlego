package ch.sbb.matsim.umlego.config.credentials;

public final class SnowflakeCredentials {

    private SnowflakeCredentials() {}

    public static String getUserName() {
        return System.getenv("SNOWFLAKE-STD-ID");
    }

    public static String getPassword() {
        return System.getenv("SNOWFLAKE-STD-SECRET");
    }
}
