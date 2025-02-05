package wd4j.impl.websocket;

import com.google.gson.JsonObject;
import wd4j.impl.markerInterfaces.ResultData;

public class CommandResponse implements Message {
    private String type = "success";
    private int id;
    private ResultData result; // Enthält die spezifischen Daten der Antwort

    @Override
    public String getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public ResultData getResult() {
        return result;
    }
}
