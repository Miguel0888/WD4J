package wd4j.core;

import wd4j.impl.module.SessionService;

import java.util.function.Consumer;

public interface Dispatcher {

    void process(String eventMessage);

    <T> void addEventListener(String eventType, Consumer<T> listener, Class<T> eventTypeClass, SessionService sessionService);

    <T> void removeEventListener(String eventType, Consumer<T> listener, SessionService sessionService);
}
