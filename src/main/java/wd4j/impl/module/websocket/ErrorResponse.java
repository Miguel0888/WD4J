package wd4j.impl.module.websocket;

public class ErrorResponse implements Message {
    private String type;
    private Integer id; // Kann null sein, wenn kein `id`-Feld vorhanden ist
    private String error;
    private String message;
    private String stacktrace;

    @Override
    public String getType() {
        return type;
    }

    public Integer getId() {
        return id;
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
}
