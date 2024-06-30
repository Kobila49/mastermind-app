package igor.kos.mastermind.exception;

public class WrongPlayerNameException extends RuntimeException {

    public WrongPlayerNameException() {
    }

    public WrongPlayerNameException(String message) {
        super(message);
    }

    public WrongPlayerNameException(String message, Throwable cause) {
        super(message, cause);
    }

    public WrongPlayerNameException(Throwable cause) {
        super(cause);
    }

    public WrongPlayerNameException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
