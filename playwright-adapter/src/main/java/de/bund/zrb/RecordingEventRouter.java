package de.bund.zrb;

import de.bund.zrb.event.WDScriptEvent;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingEventRouter {
    private final Map<String, List<RecordingEventListener>> listeners = new HashMap<>();

    public synchronized void dispatch(WDScriptEvent.Message message) {
        String contextId = message.getParams().getSource().getContext().value();
        List<RecordingEventListener> contextListeners = listeners.get(contextId);
        if (contextListeners != null) {
            for (RecordingEventListener listener : contextListeners) {
                listener.onRecordingEvent(message);
            }
        }
    }

    public synchronized void addListener(String contextId, RecordingEventListener listener) {
        listeners
                .computeIfAbsent(contextId, id -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    public synchronized void removeListener(String contextId, RecordingEventListener listener) {
        List<RecordingEventListener> contextListeners = listeners.get(contextId);
        if (contextListeners != null) {
            contextListeners.remove(listener);
            if (contextListeners.isEmpty()) {
                listeners.remove(contextId);
            }
        }
    }

    public interface RecordingEventListener {
        void onRecordingEvent(WDScriptEvent.Message message);
    }
}
