package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import de.bund.zrb.PageImpl;
import de.bund.zrb.WebDriver;
import de.bund.zrb.meta.MetaEvent;
import de.bund.zrb.meta.MetaEventService;
import de.bund.zrb.meta.MetaEventServiceImpl;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Installiert Playwright- und BiDi-Listener und publiziert MetaEvents in die UI-Textbox (MetaEventService).
 * - Playwright onX/offX: Request/Response + DOM Meilensteine
 * - BiDi-only: fragmentNavigated/historyUpdated/navigationCommitted/Aborted/Failed/Started
 *
 * Jeder Auslöser hat genau EINEN klaren Namen:
 *   BEFORE_REQUEST_SENT  -> NETWORK_REQUEST_SENT
 *   RESPONSE_STARTED     -> NETWORK_RESPONSE_STARTED
 *   RESPONSE_COMPLETED   -> NETWORK_RESPONSE_FINISHED
 *   FETCH_ERROR          -> NETWORK_REQUEST_FAILED
 *   DOM_CONTENT_LOADED   -> DOM_READY
 *   LOAD                 -> PAGE_LOADED
 *   NAVIGATION_STARTED   -> NAVIGATION_STARTED
 *   FRAGMENT_NAVIGATED   -> URL_FRAGMENT_CHANGED
 *   HISTORY_UPDATED      -> HISTORY_CHANGED
 *   NAVIGATION_COMMITTED -> NAVIGATION_COMMITTED
 *   NAVIGATION_ABORTED   -> NAVIGATION_ABORTED
 *   NAVIGATION_FAILED    -> NAVIGATION_FAILED
 */
public final class MetaHookInstaller {

    private final MetaEventService events;

    // Für sauberes Abhängen speichern wir für jedes Target alle Off-Aktionen.
    private final Map<Object, List<Runnable>> detachActions = new ConcurrentHashMap<Object, List<Runnable>>();

    public MetaHookInstaller() {
        this(MetaEventServiceImpl.getInstance());
    }

    public MetaHookInstaller(MetaEventService events) {
        this.events = events;
    }

    /** Installiert Hooks auf einem BrowserContext und allen vorhandenen/neuen Pages. */
    public void installOnContext(final BrowserContext ctx) {
        if (ctx == null) return;
        final List<Runnable> detach = bucket(ctx);

        // ---------- Netzwerk (Kontext-weit, via Playwright) ----------
        final Consumer<Request> onReq = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String, String> d = new LinkedHashMap<String, String>();
                putIfNotNull(d, "url", r.url());
                putIfNotNull(d, "method", r.method());
                putIfNotNull(d, "resourceType", r.resourceType());
                events.publish(MetaEvent.of(MetaEvent.Kind.NETWORK_REQUEST_SENT, d));
            }
        };
        ctx.onRequest(onReq);
        detach.add(new Runnable() { public void run() { ctx.offRequest(onReq); } });

        final Consumer<Response> onRespStart = new Consumer<Response>() {
            public void accept(Response resp) {
                Map<String, String> d = new LinkedHashMap<String, String>();
                putIfNotNull(d, "url", resp.url());
                d.put("status", String.valueOf(resp.status()));
                events.publish(MetaEvent.of(MetaEvent.Kind.NETWORK_RESPONSE_STARTED, d));
            }
        };
        ctx.onResponse(onRespStart);
        detach.add(new Runnable() { public void run() { ctx.offResponse(onRespStart); } });

        final Consumer<Request> onReqFinished = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String, String> d = new LinkedHashMap<String, String>();
                putIfNotNull(d, "url", r.url());
                Response resp = r.response();
                d.put("status", String.valueOf(resp != null ? resp.status() : -1));
                events.publish(MetaEvent.of(MetaEvent.Kind.NETWORK_RESPONSE_FINISHED, d));
            }
        };
        ctx.onRequestFinished(onReqFinished);
        detach.add(new Runnable() { public void run() { ctx.offRequestFinished(onReqFinished); } });

        final Consumer<Request> onReqFailed = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String, String> d = new LinkedHashMap<String, String>();
                putIfNotNull(d, "url", r.url());
                putIfNotNull(d, "error", r.failure());
                d.put("status", "FAILED");
                events.publish(MetaEvent.of(MetaEvent.Kind.NETWORK_REQUEST_FAILED, d));
            }
        };
        ctx.onRequestFailed(onReqFailed);
        detach.add(new Runnable() { public void run() { ctx.offRequestFailed(onReqFailed); } });

        // Bereits offene Pages anhängen
        List<Page> pages = ctx.pages();
        if (pages != null) for (Page p : pages) installOnPage(p);

        // Neue Pages beobachten
        final Consumer<Page> onPage = new Consumer<Page>() {
            public void accept(Page page) { installOnPage(page); }
        };
        ctx.onPage(onPage);
        detach.add(new Runnable() { public void run() { ctx.offPage(onPage); } });
    }

    /** Installiert Hooks auf einer einzelnen Page (DOM + optional Netzwerk + BiDi-only Navigation). */
    public void installOnPage(final Page page) {
        if (page == null) return;
        final List<Runnable> detach = bucket(page);

        // ---------- DOM Meilensteine ----------
        final Consumer<Page> onDom = new Consumer<Page>() {
            public void accept(Page p) { events.publish(MetaEvent.of(MetaEvent.Kind.DOM_READY)); }
        };
        page.onDOMContentLoaded(onDom);
        detach.add(new Runnable() { public void run() { page.offDOMContentLoaded(onDom); } });

        final Consumer<Page> onLoad = new Consumer<Page>() {
            public void accept(Page p) { events.publish(MetaEvent.of(MetaEvent.Kind.PAGE_LOADED)); }
        };
        page.onLoad(onLoad);
        detach.add(new Runnable() { public void run() { page.offLoad(onLoad); } });

        // ---------- Optional: Page-spezifisches Netzwerk (redundant zu Context, aber nützlich pro Seite) ----------
        final Consumer<Request> onReq = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String, String> d = new LinkedHashMap<String, String>();
                putIfNotNull(d, "url", r.url());
                putIfNotNull(d, "method", r.method());
                putIfNotNull(d, "resourceType", r.resourceType());
                events.publish(MetaEvent.of(MetaEvent.Kind.NETWORK_REQUEST_SENT, d));
            }
        };
        page.onRequest(onReq);
        detach.add(new Runnable() { public void run() { page.offRequest(onReq); } });

        final Consumer<Response> onRespStart = new Consumer<Response>() {
            public void accept(Response resp) {
                Map<String, String> d = new LinkedHashMap<String, String>();
                putIfNotNull(d, "url", resp.url());
                d.put("status", String.valueOf(resp.status()));
                events.publish(MetaEvent.of(MetaEvent.Kind.NETWORK_RESPONSE_STARTED, d));
            }
        };
        page.onResponse(onRespStart);
        detach.add(new Runnable() { public void run() { page.offResponse(onRespStart); } });

        final Consumer<Request> onReqFinished = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String, String> d = new LinkedHashMap<String, String>();
                putIfNotNull(d, "url", r.url());
                Response resp = r.response();
                d.put("status", String.valueOf(resp != null ? resp.status() : -1));
                events.publish(MetaEvent.of(MetaEvent.Kind.NETWORK_RESPONSE_FINISHED, d));
            }
        };
        page.onRequestFinished(onReqFinished);
        detach.add(new Runnable() { public void run() { page.offRequestFinished(onReqFinished); } });

        final Consumer<Request> onReqFailed = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String, String> d = new LinkedHashMap<String, String>();
                putIfNotNull(d, "url", r.url());
                putIfNotNull(d, "error", r.failure());
                d.put("status", "FAILED");
                events.publish(MetaEvent.of(MetaEvent.Kind.NETWORK_REQUEST_FAILED, d));
            }
        };
        page.onRequestFailed(onReqFailed);
        detach.add(new Runnable() { public void run() { page.offRequestFailed(onReqFailed); } });

        // ---------- BiDi-only Navigation (per Kontext-ID) ----------
        if (page instanceof PageImpl) {
            final PageImpl p = (PageImpl) page;
            final WebDriver wd = p.getWebDriver();
            final String ctxId = p.getBrowsingContextId();

            // navigationStarted
            final Consumer<Object> onNavStarted = new Consumer<Object>() {
                public void accept(Object ev) {
                    Map<String, String> d = new LinkedHashMap<String, String>();
                    putIfNotNull(d, "url", safeUrl(page));
                    events.publish(MetaEvent.of(MetaEvent.Kind.NAVIGATION_STARTED, d));
                }
            };
            subscribe(wd, WDEventNames.NAVIGATION_STARTED.getName(), ctxId, onNavStarted, detach);

            // fragmentNavigated
            final Consumer<Object> onFragment = new Consumer<Object>() {
                public void accept(Object ev) {
                    Map<String, String> d = new LinkedHashMap<String, String>();
                    putIfNotNull(d, "url", safeUrl(page));
                    events.publish(MetaEvent.of(MetaEvent.Kind.URL_FRAGMENT_CHANGED, d));
                }
            };
            subscribe(wd, WDEventNames.FRAGMENT_NAVIGATED.getName(), ctxId, onFragment, detach);

            // historyUpdated
            final Consumer<Object> onHistory = new Consumer<Object>() {
                public void accept(Object ev) {
                    Map<String, String> d = new LinkedHashMap<String, String>();
                    putIfNotNull(d, "url", safeUrl(page));
                    events.publish(MetaEvent.of(MetaEvent.Kind.HISTORY_CHANGED, d));
                }
            };
            subscribe(wd, WDEventNames.HISTORY_UPDATED.getName(), ctxId, onHistory, detach);

            // navigationCommitted
            final Consumer<Object> onCommitted = new Consumer<Object>() {
                public void accept(Object ev) {
                    Map<String, String> d = new LinkedHashMap<String, String>();
                    putIfNotNull(d, "url", safeUrl(page));
                    events.publish(MetaEvent.of(MetaEvent.Kind.NAVIGATION_COMMITTED, d));
                }
            };
            subscribe(wd, WDEventNames.NAVIGATION_COMMITTED.getName(), ctxId, onCommitted, detach);

            // navigationAborted
            final Consumer<Object> onAborted = new Consumer<Object>() {
                public void accept(Object ev) {
                    Map<String, String> d = new LinkedHashMap<String, String>();
                    putIfNotNull(d, "url", safeUrl(page));
                    events.publish(MetaEvent.of(MetaEvent.Kind.NAVIGATION_ABORTED, d));
                }
            };
            subscribe(wd, WDEventNames.NAVIGATION_ABORTED.getName(), ctxId, onAborted, detach);

            // navigationFailed
            final Consumer<Object> onFailed = new Consumer<Object>() {
                public void accept(Object ev) {
                    Map<String, String> d = new LinkedHashMap<String, String>();
                    putIfNotNull(d, "url", safeUrl(page));
                    events.publish(MetaEvent.of(MetaEvent.Kind.NAVIGATION_FAILED, d));
                }
            };
            subscribe(wd, WDEventNames.NAVIGATION_FAILED.getName(), ctxId, onFailed, detach);
        }
    }

    /** Entfernt alle zuvor installierten Hooks. */
    public void uninstallAll() {
        for (Map.Entry<Object, List<Runnable>> e : detachActions.entrySet()) {
            List<Runnable> actions = e.getValue();
            for (Runnable r : actions) { try { r.run(); } catch (Throwable ignore) { } }
        }
        detachActions.clear();
    }

    // ---------- Helpers ----------

    private void subscribe(final WebDriver wd,
                           final String eventName,
                           final String contextId,
                           final Consumer<Object> handler,
                           final List<Runnable> detach) {
        WDSubscriptionRequest req = new WDSubscriptionRequest(eventName, contextId, null);
        wd.addEventListener(req, handler);
        detach.add(new Runnable() {
            public void run() { wd.removeEventListener(eventName, contextId, handler); }
        });
    }

    private List<Runnable> bucket(Object key) {
        List<Runnable> list = detachActions.get(key);
        if (list == null) {
            list = new ArrayList<Runnable>();
            detachActions.put(key, list);
        }
        return list;
    }

    private static void putIfNotNull(Map<String, String> m, String k, String v) {
        if (v != null && !v.isEmpty()) m.put(k, v);
    }

    private static String safeUrl(Page p) {
        try { return p.url(); } catch (Throwable t) { return null; }
    }
}
