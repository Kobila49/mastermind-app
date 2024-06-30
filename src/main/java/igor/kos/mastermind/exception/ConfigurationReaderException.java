package igor.kos.mastermind.exception;

public class ConfigurationReaderException extends RuntimeException {

    public ConfigurationReaderException() {
    }

    public ConfigurationReaderException(String message) {
        super(message);
    }

    public ConfigurationReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationReaderException(Throwable cause) {
        super(cause);
    }

    public ConfigurationReaderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
