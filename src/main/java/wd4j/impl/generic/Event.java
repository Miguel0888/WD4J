package wd4j.impl.generic;

import com.google.gson.JsonObject;

public interface Event {
    String getType();
    JsonObject getData();
}