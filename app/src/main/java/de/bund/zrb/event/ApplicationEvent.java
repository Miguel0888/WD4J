package de.bund.zrb.event;

public interface ApplicationEvent<T> {
    T getPayload();
}
