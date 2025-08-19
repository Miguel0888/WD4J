package de.bund.zrb;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import de.bund.zrb.event.WDScriptEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingEventRouter {

    private final Map<Page, List<RecordingEventListener>> pageListeners = new HashMap<>();
    private final Map<BrowserContext, List<RecordingEventListener>> contextListeners = new HashMap<>();
    private final BrowserImpl browser;

    public RecordingEventRouter(BrowserImpl browser) {
        this.browser = browser;
    }

    public synchronized void dispatch(WDScriptEvent.MessageWD message) {
        String contextId = message.getParams().getSource().getContext().value();

        Page page = browser.getAllPages().stream()
                .filter(p -> contextId.equals(((PageImpl) p).getBrowsingContextId()))
                .findFirst()
                .orElse(null);

        if (page == null) {
            System.err.println("⚠️ Keine Page für contextId gefunden: " + contextId);
        } else {
            List<RecordingEventListener> pageList = pageListeners.get(page);
            if (pageList != null) {
                for (RecordingEventListener l : pageList) {
                    l.onRecordingEvent(message);
                }
            }
        }

        // ➜ Context-spezifisch IMMER probieren!
        BrowserContext context = null;
        if (page != null) {
            context = page.context();
        } else {
            // Fallback: Context direkt anhand contextId auflösen
            context = browser.contexts().stream()
                    .filter(ctx -> ((UserContextImpl) ctx).getUserContext().value().equals(contextId))
                    .findFirst()
                    .orElse(null);
        }

        if (context == null) {
            System.err.println("⚠️ Kein Context für contextId gefunden: " + contextId);
        } else {
            List<RecordingEventListener> contextList = contextListeners.get(context);
            if (contextList != null) {
                for (RecordingEventListener l : contextList) {
                    l.onRecordingEvent(message);
                }
            }
        }
    }

    public synchronized void addPageListener(Page page, RecordingEventListener listener) {
        pageListeners
                .computeIfAbsent(page, id -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    public synchronized void removePageListener(Page page, RecordingEventListener listener) {
        List<RecordingEventListener> list = pageListeners.get(page);
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) {
                pageListeners.remove(page);
            }
        }
    }

    public synchronized void addContextListener(BrowserContext context, RecordingEventListener listener) {
        contextListeners
                .computeIfAbsent(context, id -> new CopyOnWriteArrayList<>())
                .add(listener);
    }

    public synchronized void removeContextListener(BrowserContext context, RecordingEventListener listener) {
        List<RecordingEventListener> list = contextListeners.get(context);
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) {
                contextListeners.remove(context);
            }
        }
    }

    public interface RecordingEventListener {
        void onRecordingEvent(WDScriptEvent.MessageWD message);
    }
}
