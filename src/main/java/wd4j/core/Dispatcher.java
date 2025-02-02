package wd4j.core;

import java.util.function.Consumer;

public interface Dispatcher {

    void processEvent(String eventMessage);

    void processResponse(String message);

    <T> void addEventListener(String eventType, Consumer<T> listener, Class<T> eventTypeClass);

}
