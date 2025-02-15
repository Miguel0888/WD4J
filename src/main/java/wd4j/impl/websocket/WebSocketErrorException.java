package wd4j.impl.websocket;

public class WebSocketErrorException extends RuntimeException {
    private final ErrorResponse errorResponse;

    public WebSocketErrorException(ErrorResponse errorResponse) {
        super("WebSocket Error: " + errorResponse.getMessage());
        this.errorResponse = errorResponse;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }
}
