package in.guardianservices.document_utility_core.exception;

public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException() {
        super("Invalid PDF password");
    }

    public InvalidPasswordException(String message) {
        super(message);
    }

    public InvalidPasswordException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPasswordException(Throwable cause) {
        super(cause);
    }
}
