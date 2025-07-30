package de.bund.zrb.model;

/**
 * Wird geworfen, wenn eine Konfiguration für eine Erwartung ungültig ist.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
