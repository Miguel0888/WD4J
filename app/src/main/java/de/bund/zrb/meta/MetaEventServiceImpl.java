package de.bund.zrb.meta;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Provide a simple in-memory event service for meta events (thread-safe). */
public final class MetaEventServiceImpl implements MetaEventService {

    private static final MetaEventServiceImpl INSTANCE = new MetaEventServiceImpl();

    private final List<MetaEventListener> listeners = new CopyOnWriteArrayList<MetaEventListener>();

    private MetaEventServiceImpl() { }

    public static MetaEventServiceImpl getInstance() {
        return INSTANCE;
    }

    public void addListener(MetaEventListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(MetaEventListener listener) {
        if (listener != null) listeners.remove(listener);
    }

    public void publish(MetaEvent event) {
        if (event == null) return;
        for (MetaEventListener l : listeners) {
            try { l.onMetaEvent(event); } catch (Throwable ignore) { /* Do not break others */ }
        }
    }
}
