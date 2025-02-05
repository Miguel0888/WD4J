package wd4j.impl.websocket;

import wd4j.impl.webdriver.type.error.ErrorCode;

public class ErrorResponse implements Message {
    private String type = "error";
    private Integer id; // Kann null sein, wenn kein `id`-Feld vorhanden ist
    private ErrorCode error;
    private String message;
    private String stacktrace;

    @Override
    public String getType() {
        return type;
    }

    public Integer getId() {
        return id;
    }

    public ErrorCode getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public String getStacktrace() {
        return stacktrace;
    }
}
