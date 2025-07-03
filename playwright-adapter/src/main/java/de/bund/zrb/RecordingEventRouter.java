package de.bund.zrb;

import com.microsoft.playwright.Page;
import de.bund.zrb.event.WDScriptEvent;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingEventRouter {
    private final Map<Page, List<RecordingEventListener>> listeners = new HashMap<>();
    private final BrowserImpl browser;

    public RecordingEventRouter(BrowserImpl browser) {
        this.browser = browser; // Pages wird aus BrowserImpl übergeben!
    }

    public synchronized void dispatch(WDScriptEvent.Message message) {
        String contextId = message.getParams().getSource().getContext().value();

        Page page = browser.getAllPages().stream()
                .filter(p -> contextId.equals(((PageImpl) p).getBrowsingContextId()))
                .findFirst()
                .orElse(null);

        if (page == null) {
            System.err.println("⚠️ Keine Page für contextId gefunden: " + contextId);
            return;
        }

        List<RecordingEventListener> pageListeners = listeners.get(page);
        if (pageListeners != null) {
            for (RecordingEventListener listener : pageListeners) {
                listener.onRecordingEvent(message);
            }
        }
    }

    public synchronized void addListener(Page page, RecordingEventListener listener) {
        listeners
                .computeIfAbsent(page, id -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    public synchronized void removeListener(Page page, RecordingEventListener listener) {
        List<RecordingEventListener> pageListeners = listeners.get(page);
        if (pageListeners != null) {
            pageListeners.remove(listener);
            if (pageListeners.isEmpty()) {
                listeners.remove(page);
            }
        }
    }

    public interface RecordingEventListener {
        void onRecordingEvent(WDScriptEvent.Message message);
    }
}
