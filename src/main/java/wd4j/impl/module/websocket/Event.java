package wd4j.impl.module.websocket;

import com.google.gson.JsonObject;

public class Event implements Message {
    private String type;
    private JsonObject params; // Die Event-Daten

    @Override
    public String getType() {
        return type;
    }

    public JsonObject getParams() {
        return params;
    }
}
