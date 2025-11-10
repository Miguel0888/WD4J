package de.bund.zrb.websocket;

import de.bund.zrb.api.WDCommandResponse;

public class WDErrorResponse extends RuntimeException implements WDCommandResponse<WDErrorResponse> {
    private String type = "error"; // Immer "error"
    private Integer id; // Kann `null` sein, wenn kein `id`-Feld vorhanden ist
    private String error;
    private String message;
    private String stacktrace;

    @Override
    public String getType() {
        return type;
    }

    @Override
    public int getId() {
        return id != null ? id : -1; // Falls `null`, setzen wir `-1` als Fehler-ID
    }

    @Override
    public WDErrorResponse getResult() {
        return this;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "error='" + error + '\'' +
                ", message='" + message + '\'' +
                ", stacktrace='" + stacktrace + '\'' +
                '}';
    }
}
