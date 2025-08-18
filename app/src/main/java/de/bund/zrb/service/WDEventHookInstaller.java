package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import de.bund.zrb.PageImpl;
import de.bund.zrb.WebDriver;
import de.bund.zrb.meta.MetaEventService;
import de.bund.zrb.meta.MetaEventServiceImpl;
import de.bund.zrb.websocket.WDEvent;
import de.bund.zrb.websocket.WDEventNames;
import de.bund.zrb.type.session.WDSubscriptionRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Installiert BiDi-Listener und leitet TYPISIERTE WDEvents an MetaEventService.publish(WDEvent<?>)
 * weiter, damit der RecorderTab (WDEventListener) sie empfangen kann.
 *
 * Keine Playwright-Hooks, keine Legacy-MetaEvents – nur BiDi forward.
 */
public final class WDEventHookInstaller {

    private final MetaEventService events;

    // Alle Off-Aktionen pro Target (Context oder Page)
    private final Map<Object, List<Runnable>> detachActions = new ConcurrentHashMap<Object, List<Runnable>>();

    public WDEventHookInstaller() {
        this(MetaEventServiceImpl.getInstance());
    }

    public WDEventHookInstaller(MetaEventService events) {
        this.events = events;
    }

    /** Installiert Forwarder für einen BrowserContext – hängt sich an alle existierenden und neuen Pages. */
    public void installOnContext(final BrowserContext ctx) {
        if (ctx == null) return;
        final List<Runnable> detach = bucket(ctx);

        // Bereits offene Pages
        final List<Page> pages = ctx.pages();
        if (pages != null) for (Page p : pages) installOnPage(p);

        // Neue Pages beobachten
        final Consumer<Page> onPage = new Consumer<Page>() {
            @Override public void accept(Page page) { installOnPage(page); }
        };
        ctx.onPage(onPage);
        detach.add(new Runnable() { @Override public void run() { ctx.offPage(onPage); } });
    }

    /** Installiert Forwarder für eine einzelne Page (abonniert BiDi-Events für deren browsingContextId). */
    public void installOnPage(final Page page) {
        if (page == null) return;
        final List<Runnable> detach = bucket(page);

        if (!(page instanceof PageImpl)) return; // nur unsere Implementierung kann ctxId/WebDriver liefern
        final PageImpl p = (PageImpl) page;
        final WebDriver wd = p.getWebDriver();
        final String ctxId = p.getBrowsingContextId();
        if (wd == null || ctxId == null) return;

        // --- Network ---
        subscribeAndForward(wd, WDEventNames.BEFORE_REQUEST_SENT.getName(),  ctxId, detach);
        subscribeAndForward(wd, WDEventNames.RESPONSE_STARTED.getName(),     ctxId, detach);
        subscribeAndForward(wd, WDEventNames.RESPONSE_COMPLETED.getName(),   ctxId, detach);
        subscribeAndForward(wd, WDEventNames.FETCH_ERROR.getName(),          ctxId, detach);
        subscribeAndForward(wd, WDEventNames.AUTH_REQUIRED.getName(),        ctxId, detach);

        // --- Script / Log ---
        subscribeAndForward(wd, WDEventNames.MESSAGE.getName(),              ctxId, detach);
        subscribeAndForward(wd, WDEventNames.REALM_CREATED.getName(),        ctxId, detach);
        subscribeAndForward(wd, WDEventNames.REALM_DESTROYED.getName(),      ctxId, detach);
        subscribeAndForward(wd, WDEventNames.ENTRY_ADDED.getName(),          ctxId, detach);

        // --- Browsing Context (DOM, Navigation, Dialoge, Downloads) ---
        subscribeAndForward(wd, WDEventNames.DOM_CONTENT_LOADED.getName(),   ctxId, detach);
        subscribeAndForward(wd, WDEventNames.LOAD.getName(),                 ctxId, detach);

        subscribeAndForward(wd, WDEventNames.NAVIGATION_STARTED.getName(),   ctxId, detach);
        subscribeAndForward(wd, WDEventNames.FRAGMENT_NAVIGATED.getName(),   ctxId, detach);
        subscribeAndForward(wd, WDEventNames.HISTORY_UPDATED.getName(),      ctxId, detach);
        subscribeAndForward(wd, WDEventNames.NAVIGATION_COMMITTED.getName(), ctxId, detach);
        subscribeAndForward(wd, WDEventNames.NAVIGATION_ABORTED.getName(),   ctxId, detach);
        subscribeAndForward(wd, WDEventNames.NAVIGATION_FAILED.getName(),    ctxId, detach);

        subscribeAndForward(wd, WDEventNames.USER_PROMPT_OPENED.getName(),   ctxId, detach);
        subscribeAndForward(wd, WDEventNames.USER_PROMPT_CLOSED.getName(),   ctxId, detach);
        subscribeAndForward(wd, WDEventNames.DOWNLOAD_WILL_BEGIN.getName(),  ctxId, detach);
    }

    /** Entfernt alle zuvor installierten BiDi-Forwarder. */
    public void uninstallAll() {
        for (Map.Entry<Object, List<Runnable>> e : detachActions.entrySet()) {
            for (Runnable r : e.getValue()) { try { r.run(); } catch (Throwable ignore) { } }
        }
        detachActions.clear();
    }

    // ---------- intern ----------

    private void subscribeAndForward(final WebDriver wd,
                                     final String eventName,
                                     final String contextId,
                                     final List<Runnable> detach) {
        final Consumer<Object> forward = new Consumer<Object>() {
            @Override public void accept(Object ev) {
                if (ev instanceof WDEvent<?>) {
                    events.publish((WDEvent<?>) ev); // <-- TYPISCHES Publish triggert RecorderTab
                }
            }
        };
        final WDSubscriptionRequest req = new WDSubscriptionRequest(eventName, contextId, null);
        wd.addEventListener(req, forward);
        detach.add(new Runnable() { @Override public void run() { wd.removeEventListener(eventName, contextId, forward); } });
    }

    private List<Runnable> bucket(Object key) {
        List<Runnable> list = detachActions.get(key);
        if (list == null) {
            list = new ArrayList<Runnable>();
            detachActions.put(key, list);
        }
        return list;
    }
}
