package wd4j.impl.module.websocket;

import com.google.gson.JsonObject;

public class CommandResponse implements Message {
    private String type;
    private int id;
    private JsonObject result; // Enthält die spezifischen Daten der Antwort

    @Override
    public String getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public JsonObject getResult() {
        return result;
    }
}
