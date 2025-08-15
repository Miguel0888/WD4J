package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import de.bund.zrb.PageImpl;
import de.bund.zrb.meta.MetaEvent;
import de.bund.zrb.meta.MetaEventService;
import de.bund.zrb.meta.MetaEventServiceImpl;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Installiert Playwright/BiDi-Hooks und publiziert MetaEvents für den UI-Drawer. */
public final class MetaHookInstaller {

    private final MetaEventService events;

    /** pro Context/Page die passenden Off-Actions, damit sauber entfernt werden kann */
    private final Map<Object, List<Runnable>> detachActions = new ConcurrentHashMap<Object, List<Runnable>>();

    public MetaHookInstaller() {
        this(MetaEventServiceImpl.getInstance());
    }

    public MetaHookInstaller(MetaEventService events) {
        this.events = events;
    }

    /** Context-weit: Netzwerk + „neue Seite“-Hook; außerdem bestehende Seiten nachrüsten. */
    public void installOnContext(final BrowserContext ctx) {
        if (ctx == null) return;

        final List<Runnable> detach = bucket(ctx);

        // --- Netzwerk (context-weit)
        final Consumer<Request> onReq = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String,String> d = new HashMap<String,String>();
                d.put("url", nvl(r.url()));
                if (r.method() != null) d.put("method", r.method());
                if (r.resourceType() != null) d.put("type", r.resourceType());
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_STARTED, d));
            }
        };
        ctx.onRequest(onReq);
        detach.add(new Runnable() { public void run() { ctx.offRequest(onReq); } });

        final Consumer<Response> onResp = new Consumer<Response>() {
            public void accept(Response r) {
                Map<String,String> d = new HashMap<String,String>();
                d.put("url", nvl(r.url()));
                d.put("status", String.valueOf(r.status()));
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_COMPLETED, d));
            }
        };
        ctx.onResponse(onResp);
        detach.add(new Runnable() { public void run() { ctx.offResponse(onResp); } });

        final Consumer<Request> onReqFailed = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String,String> d = new HashMap<String,String>();
                d.put("url", nvl(r.url()));
                d.put("status", "FAILED");
                d.put("error", nvl(r.failure()));
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_COMPLETED, d));
            }
        };
        ctx.onRequestFailed(onReqFailed);
        detach.add(new Runnable() { public void run() { ctx.offRequestFailed(onReqFailed); } });

        // bereits offene Seiten nachrüsten
        List<Page> pages = ctx.pages();
        if (pages != null) for (Page p : pages) installOnPage(p);

        // neue Seiten automatisch bestücken
        final Consumer<Page> onPage = new Consumer<Page>() {
            public void accept(Page page) { installOnPage(page); }
        };
        ctx.onPage(onPage);
        detach.add(new Runnable() { public void run() { ctx.offPage(onPage); } });
    }

    /** Page-spezifische Hooks (Load-Meilensteine, Frame-Navigation, optional Netz-Events, BiDi-Extras). */
    public void installOnPage(final Page page) {
        if (page == null) return;

        final List<Runnable> detach = bucket(page);

        // --- LOAD
        final Consumer<Page> onLoad = new Consumer<Page>() {
            public void accept(Page p) { events.publish(MetaEvent.of(MetaEvent.Kind.LOAD)); }
        };
        page.onLoad(onLoad);
        detach.add(new Runnable() { public void run() { page.offLoad(onLoad); } });

        // --- DOMContentLoaded
        final Consumer<Page> onDom = new Consumer<Page>() {
            public void accept(Page p) { events.publish(MetaEvent.of(MetaEvent.Kind.DOMCONTENTLOADED)); }
        };
        page.onDOMContentLoaded(onDom);
        detach.add(new Runnable() { public void run() { page.offDOMContentLoaded(onDom); } });

        // --- Main-Frame navigiert → URL_CHANGED
        final Consumer<Frame> onFrameNav = new Consumer<Frame>() {
            public void accept(Frame f) {
                try {
                    if (f == page.mainFrame()) {
                        Map<String,String> d = new HashMap<String,String>();
                        d.put("url", nvl(page.url()));
                        events.publish(MetaEvent.of(MetaEvent.Kind.URL_CHANGED, d));
                    }
                } catch (Throwable ignore) { }
            }
        };
        page.onFrameNavigated(onFrameNav);
        detach.add(new Runnable() { public void run() { page.offFrameNavigated(onFrameNav); } });

        // --- optionale Page-Netzwerk-Events (nützlich pro Seite, zusätzlich zu Context-Hooks)
        final Consumer<Request> onReq = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String,String> d = new HashMap<String,String>();
                d.put("url", nvl(r.url()));
                if (r.method() != null) d.put("method", r.method());
                if (r.resourceType() != null) d.put("type", r.resourceType());
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_STARTED, d));
            }
        };
        page.onRequest(onReq);
        detach.add(new Runnable() { public void run() { page.offRequest(onReq); } });

        final Consumer<Response> onResp = new Consumer<Response>() {
            public void accept(Response r) {
                Map<String,String> d = new HashMap<String,String>();
                d.put("url", nvl(r.url()));
                d.put("status", String.valueOf(r.status()));
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_COMPLETED, d));
            }
        };
        page.onResponse(onResp);
        detach.add(new Runnable() { public void run() { page.offResponse(onResp); } });

        final Consumer<Request> onReqFailed = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String,String> d = new HashMap<String,String>();
                d.put("url", nvl(r.url()));
                d.put("status", "FAILED");
                d.put("error", nvl(r.failure()));
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_COMPLETED, d));
            }
        };
        page.onRequestFailed(onReqFailed);
        detach.add(new Runnable() { public void run() { page.offRequestFailed(onReqFailed); } });

        // --- Dialoge (PROMPT_OPENED) – promptClosed kommt über BiDi unten
        final Consumer<com.microsoft.playwright.Dialog> onDialog = new Consumer<com.microsoft.playwright.Dialog>() {
            public void accept(com.microsoft.playwright.Dialog dialog) {
                Map<String,String> d = new HashMap<String,String>();
                d.put("type", nvl(dialog.type()));
                d.put("message", nvl(dialog.message()));
                events.publish(MetaEvent.of(MetaEvent.Kind.PROMPT_OPENED, d));
            }
        };
        page.onDialog(onDialog);
        detach.add(new Runnable() { public void run() { page.offDialog(onDialog); } });

        // --- BiDi-Only: Fragment-Navigation, History-Update, Commit/Abort, PromptClosed, AuthRequired
        installBiDiExtrasOnPage(page, detach);
    }

    /** Entfernt alle installierten Hooks dieser Installer-Instanz. */
    public void uninstallAll() {
        for (Map.Entry<Object,List<Runnable>> e : detachActions.entrySet()) {
            for (Runnable r : e.getValue()) {
                try { r.run(); } catch (Throwable ignore) { }
            }
        }
        detachActions.clear();
    }

    // ------------------------- helpers -------------------------

    private void installBiDiExtrasOnPage(final Page page, final List<Runnable> detach) {
        if (!(page instanceof PageImpl)) return;

        final PageImpl p = (PageImpl) page;
        final de.bund.zrb.WebDriver wd = p.getWebDriver();
        final String ctxId = p.getBrowsingContextId();

        // FRAGMENT_NAVIGATED → URL_CHANGED(reason=fragment)
        final Consumer<Object> onFragment = new Consumer<Object>() {
            public void accept(Object ev) {
                Map<String,String> d = new HashMap<String,String>();
                d.put("reason", "fragment");
                d.put("url", nvl(page.url()));
                events.publish(MetaEvent.of(MetaEvent.Kind.URL_CHANGED, d));
            }
        };
        subscribe(wd, WDEventNames.FRAGMENT_NAVIGATED.getName(), ctxId, onFragment, detach);

        // HISTORY_UPDATED → URL_CHANGED(reason=history)
        final Consumer<Object> onHistory = new Consumer<Object>() {
            public void accept(Object ev) {
                Map<String,String> d = new HashMap<String,String>();
                d.put("reason", "historyUpdated");
                d.put("url", nvl(page.url()));
                events.publish(MetaEvent.of(MetaEvent.Kind.URL_CHANGED, d));
            }
        };
        subscribe(wd, WDEventNames.HISTORY_UPDATED.getName(), ctxId, onHistory, detach);

        // NAVIGATION_COMMITTED → URL_CHANGED(reason=committed)
        final Consumer<Object> onCommit = new Consumer<Object>() {
            public void accept(Object ev) {
                Map<String,String> d = new HashMap<String,String>();
                d.put("reason", "committed");
                d.put("url", nvl(page.url()));
                events.publish(MetaEvent.of(MetaEvent.Kind.URL_CHANGED, d));
            }
        };
        subscribe(wd, WDEventNames.NAVIGATION_COMMITTED.getName(), ctxId, onCommit, detach);

        // NAVIGATION_ABORTED → URL_CHANGED(reason=aborted)
        final Consumer<Object> onAbort = new Consumer<Object>() {
            public void accept(Object ev) {
                Map<String,String> d = new HashMap<String,String>();
                d.put("reason", "aborted");
                d.put("url", nvl(page.url()));
                events.publish(MetaEvent.of(MetaEvent.Kind.URL_CHANGED, d));
            }
        };
        subscribe(wd, WDEventNames.NAVIGATION_ABORTED.getName(), ctxId, onAbort, detach);

        // USER_PROMPT_CLOSED → PROMPT_CLOSED
        final Consumer<Object> onPromptClosed = new Consumer<Object>() {
            public void accept(Object ev) {
                events.publish(MetaEvent.of(MetaEvent.Kind.PROMPT_CLOSED));
            }
        };
        subscribe(wd, WDEventNames.USER_PROMPT_CLOSED.getName(), ctxId, onPromptClosed, detach);

        // AUTH_REQUIRED → AUTH_REQUIRED (falls vorhanden)
        final Consumer<Object> onAuth = new Consumer<Object>() {
            public void accept(Object ev) {
                Map<String,String> d = new HashMap<String,String>();
                // Mapper liefert für authRequired ein Request-ähnliches Objekt → URL falls möglich mitgeben
                if (ev instanceof Request) {
                    Request r = (Request) ev;
                    d.put("url", nvl(r.url()));
                    if (r.method() != null) d.put("method", r.method());
                }
                events.publish(MetaEvent.of(MetaEvent.Kind.AUTH_REQUIRED, d.isEmpty() ? null : d));
            }
        };
        subscribe(wd, WDEventNames.AUTH_REQUIRED.getName(), ctxId, onAuth, detach);
    }

    private void subscribe(final de.bund.zrb.WebDriver wd,
                           final String eventName,
                           final String contextId,
                           final Consumer<Object> handler,
                           final List<Runnable> detach) {
        WDSubscriptionRequest req = new WDSubscriptionRequest(eventName, contextId, null);
        wd.addEventListener(req, handler);
        detach.add(new Runnable() { public void run() { wd.removeEventListener(eventName, contextId, handler); } });
    }

    private List<Runnable> bucket(Object key) {
        List<Runnable> list = detachActions.get(key);
        if (list == null) {
            list = new ArrayList<Runnable>();
            detachActions.put(key, list);
        }
        return list;
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
