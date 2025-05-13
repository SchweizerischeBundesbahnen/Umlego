package ch.sbb.matsim.umlego.config;

public enum ExitCode {
    SUCCESS(0),
    FAILED(1),
    INVALID_INPUT_FOLDER(3),
    SQL_ERROR(4),
    SAISON_BASE_ID_NOT_FOUND(5),
    RUN_ID_ALREADY_EXISTS(6),
    PARSING_ERROR(7),
    AUTOMATIC_VALIDATION_FAILED(20);

    private final int code;

    private ExitCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
