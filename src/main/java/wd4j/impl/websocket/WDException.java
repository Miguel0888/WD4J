package wd4j.impl.websocket;

public class WDException extends RuntimeException {
    private final WDErrorResponseWD WDErrorResponse;

    public WDException(WDErrorResponseWD WDErrorResponse) {
        super("WebSocket Error: " + WDErrorResponse.getMessage());
        this.WDErrorResponse = WDErrorResponse;
    }

    public WDErrorResponseWD getErrorResponse() {
        return WDErrorResponse;
    }
}
