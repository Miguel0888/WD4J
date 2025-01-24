package wd4j.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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

    /**
     * Converts the command to a JsonObject.
     *
     * @return JsonObject representation of the command
     */
    public JsonObject toJsonObject() {
        Gson gson = new Gson();
        return gson.toJsonTree(this).getAsJsonObject();
    }

    /**
     * Converts the command to a JSON string.
     *
     * @return JSON string representation of the command
     */
    public String toJsonString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
