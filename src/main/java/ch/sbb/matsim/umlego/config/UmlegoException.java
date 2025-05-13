package ch.sbb.matsim.umlego.config;

/**
 * Generic exception for global error handling
 */
public class UmlegoException extends RuntimeException {

    private final ExitCode exitCode;

    public UmlegoException(String message, Throwable cause, ExitCode exitCode) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public UmlegoException(String message, ExitCode exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return this.exitCode.getCode();
    }
}
