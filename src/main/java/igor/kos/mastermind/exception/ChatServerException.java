package igor.kos.mastermind.exception;

public class ChatServerException extends RuntimeException {

    public ChatServerException() {
    }

    public ChatServerException(String message) {
        super(message);
    }

    public ChatServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public ChatServerException(Throwable cause) {
        super(cause);
    }

    public ChatServerException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
