package de.bund.zrb.websocket;

public class WDException extends RuntimeException {
    private final WDErrorResponse errorResponse;

    public WDException(WDErrorResponse errorResponse) {
        super("WebSocket Error: " + errorResponse.getMessage());
        this.errorResponse = errorResponse;
    }

    public WDErrorResponse getErrorResponse() {
        return errorResponse;
    }
}
