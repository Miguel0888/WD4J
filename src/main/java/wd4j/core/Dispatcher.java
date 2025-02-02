package wd4j.core;

public interface Dispatcher {

    void processEvent(String message);

    void processResponse(String message);
}
