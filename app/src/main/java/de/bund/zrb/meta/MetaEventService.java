package de.bund.zrb.meta;

/** Publish and subscribe to meta events (experimental). */
public interface MetaEventService {
    void addListener(MetaEventListener listener);
    void removeListener(MetaEventListener listener);
    void publish(MetaEvent event);
}
