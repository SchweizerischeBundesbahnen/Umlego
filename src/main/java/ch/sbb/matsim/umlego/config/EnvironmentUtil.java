package ch.sbb.matsim.umlego.config;

import org.apache.commons.lang3.StringUtils;

public class EnvironmentUtil {

    private EnvironmentUtil() {}

    public static String getStage() {
        String stage = System.getenv("STAGE");
        return (StringUtils.isNotEmpty(stage)) ? stage.toLowerCase() : "devx";
    }
}
