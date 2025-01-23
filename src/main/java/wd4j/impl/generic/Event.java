package wd4j.impl.generic;

public interface Event {
    String getType();
    JsonObject getData();
}