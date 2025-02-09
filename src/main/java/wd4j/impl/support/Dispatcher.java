package wd4j.impl.support;

import wd4j.impl.service.SessionService;

import java.util.function.Consumer;

public interface Dispatcher {

    void process(String eventMessage);

    <T> void addEventListener(String eventType, Consumer<T> listener, Class<T> eventTypeClass, SessionService sessionService);

    <T> void removeEventListener(String eventType, Consumer<T> listener, SessionService sessionService);
}
