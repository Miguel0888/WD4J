package wd4j.impl;

import com.google.gson.Gson;

public class Command {
    private final int id;
    private final String method;
    private final Object params;

    public Command(WebSocketConnection webSocketConnection, String method, Object params) {
        this.id = webSocketConnection.getNextCommandId();
        this.method = method;
        this.params = params;
    }

    public int getId() {
        return id;
    }

    public String getMethod() {
        return method;
    }

    public Object getParams() {
        return params;
    }
}
