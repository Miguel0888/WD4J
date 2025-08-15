package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Frame;

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
 * Installiert Playwright- und (zusätzlich) BiDi-Hooks und publiziert NUR die
 * vom Nutzer gewünschten Meta-Events in die bestehende Textbox.
 *
 * Sichtbare Events in der UI:
 *  - NAVIGATION_STARTED
 *  - FRAGMENT_NAVIGATED
 *  - HISTORY_UPDATED
 *  - DOM_CONTENT_LOADED
 *  - LOAD
 *  - NAVIGATION_ABORTED
 *  - NAVIGATION_COMMITTED
 *  - NAVIGATION_FAILED
 *  - BEFORE_REQUEST_SENT
 *  - FETCH_ERROR
 *  - RESPONSE_COMPLETED
 *  - RESPONSE_STARTED
 *
 * Keine Prompts, keine sonstigen Events.
 */
public final class MetaHookInstaller {

    private final MetaEventService events;

    /** pro Schlüssel (Context oder Page) Liste von "Abmeldungen" (offX / removeEventListener) */
    private final Map<Object, List<Runnable>> detachActions = new ConcurrentHashMap<Object, List<Runnable>>();

    public MetaHookInstaller() {
        this(MetaEventServiceImpl.getInstance());
    }

    public MetaHookInstaller(MetaEventService events) {
        this.events = events;
    }

    // =====================================================================================
    // Public API
    // =====================================================================================

    /** Context-Modus: Netzwerk-Hooks (context-weit) + Weitergabe neuer Pages an installOnPage */
    public void installOnContext(final BrowserContext ctx) {
        if (ctx == null) return;
        final List<Runnable> detach = bucket(ctx);

        // -------------------- Netzwerk (Playwright) --------------------
        // BEFORE_REQUEST_SENT
        final Consumer<Request> onReq = new Consumer<Request>() {
            @Override public void accept(Request r) {
                Map<String,String> d = new LinkedHashMap<String,String>();
                put(d, "method", r.method());
                put(d, "type",   r.resourceType());
                put(d, "url",    r.url());
                publish("BEFORE_REQUEST_SENT", d);
            }
        };
        ctx.onRequest(onReq);
        detach.add(new Runnable() { @Override public void run() { ctx.offRequest(onReq); } });

        // RESPONSE_STARTED
        final Consumer<Response> onRespStart = new Consumer<Response>() {
            @Override public void accept(Response resp) {
                Map<String,String> d = new LinkedHashMap<String,String>();
                put(d, "url", resp.url());
                put(d, "status", String.valueOf(resp.status()));
                publish("RESPONSE_STARTED", d);
            }
        };
        ctx.onResponse(onRespStart);
        detach.add(new Runnable() { @Override public void run() { ctx.offResponse(onRespStart); } });

        // RESPONSE_COMPLETED
        final Consumer<Request> onReqFinished = new Consumer<Request>() {
            @Override public void accept(Request r) {
                Response resp = r.response();
                Map<String,String> d = new LinkedHashMap<String,String>();
                put(d, "url", r.url());
                put(d, "status", (resp != null ? String.valueOf(resp.status()) : "-1"));
                publish("RESPONSE_COMPLETED", d);
            }
        };
        ctx.onRequestFinished(onReqFinished);
        detach.add(new Runnable() { @Override public void run() { ctx.offRequestFinished(onReqFinished); } });

        // FETCH_ERROR
        final Consumer<Request> onReqFailed = new Consumer<Request>() {
            @Override public void accept(Request r) {
                Map<String,String> d = new LinkedHashMap<String,String>();
                put(d, "url", r.url());
                put(d, "error", nvl(r.failure()));
                publish("FETCH_ERROR", d);
            }
        };
        ctx.onRequestFailed(onReqFailed);
        detach.add(new Runnable() { @Override public void run() { ctx.offRequestFailed(onReqFailed); } });

        // -------------------- bereits offene Pages (für DOM+Nav) --------------------
        List<Page> pages = ctx.pages();
        if (pages != null) {
            for (Page p : pages) {
                installOnPage(p);
            }
        }

        // neue Pages
        final Consumer<Page> onPage = new Consumer<Page>() {
            @Override public void accept(Page p) { installOnPage(p); }
        };
        ctx.onPage(onPage);
        detach.add(new Runnable() { @Override public void run() { ctx.offPage(onPage); } });
    }

    /** Page-Modus: DOM-Meilensteine (Playwright) + BiDi-Only Navigationsereignisse + (optional) Netzwerk pro Page */
    public void installOnPage(final Page page) {
        if (page == null) return;
        final List<Runnable> detach = bucket(page);

        // -------------------- DOM (Playwright) --------------------
        // DOM_CONTENT_LOADED
        final Consumer<Page> onDom = new Consumer<Page>() {
            @Override public void accept(Page p) {
                publish("DOM_CONTENT_LOADED", null);
            }
        };
        page.onDOMContentLoaded(onDom);
        detach.add(new Runnable() { @Override public void run() { page.offDOMContentLoaded(onDom); } });

        // LOAD
        final Consumer<Page> onLoad = new Consumer<Page>() {
            @Override public void accept(Page p) {
                publish("LOAD", null);
            }
        };
        page.onLoad(onLoad);
        detach.add(new Runnable() { @Override public void run() { page.offLoad(onLoad); } });

        // -------------------- Navigation Start (Playwright ersatzweise über Frame-Navigation) --------------------
        // Wir wollen NAVIGATION_STARTED taggen. Playwright bietet dafür kein direktes onX,
        // aber frameNavigated ist das nächstliegende Signal. Nur Main-Frame berücksichtigen.
        final Consumer<Frame> onFrameNav = new Consumer<Frame>() {
            @Override public void accept(Frame f) {
                try {
                    if (f == page.mainFrame()) {
                        Map<String,String> d = new LinkedHashMap<String,String>();
                        put(d, "url", safeUrl(page));
                        publish("NAVIGATION_STARTED", d);
                    }
                } catch (Throwable ignore) {}
            }
        };
        page.onFrameNavigated(onFrameNav);
        detach.add(new Runnable() { @Override public void run() { page.offFrameNavigated(onFrameNav); } });

        // -------------------- Netzwerk (Playwright) – Page-lokal (für Page-Modus nützlich) --------------------
        final Consumer<Request> onReq = new Consumer<Request>() {
            @Override public void accept(Request r) {
                Map<String,String> d = new LinkedHashMap<String,String>();
                put(d, "method", r.method());
                put(d, "type",   r.resourceType());
                put(d, "url",    r.url());
                publish("BEFORE_REQUEST_SENT", d);
            }
        };
        page.onRequest(onReq);
        detach.add(new Runnable() { @Override public void run() { page.offRequest(onReq); } });

        final Consumer<Response> onRespStart = new Consumer<Response>() {
            @Override public void accept(Response resp) {
                Map<String,String> d = new LinkedHashMap<String,String>();
                put(d, "url", resp.url());
                put(d, "status", String.valueOf(resp.status()));
                publish("RESPONSE_STARTED", d);
            }
        };
        page.onResponse(onRespStart);
        detach.add(new Runnable() { @Override public void run() { page.offResponse(onRespStart); } });

        final Consumer<Request> onReqFinished = new Consumer<Request>() {
            @Override public void accept(Request r) {
                Response resp = r.response();
                Map<String,String> d = new LinkedHashMap<String,String>();
                put(d, "url", r.url());
                put(d, "status", (resp != null ? String.valueOf(resp.status()) : "-1"));
                publish("RESPONSE_COMPLETED", d);
            }
        };
        page.onRequestFinished(onReqFinished);
        detach.add(new Runnable() { @Override public void run() { page.offRequestFinished(onReqFinished); } });

        final Consumer<Request> onReqFailed = new Consumer<Request>() {
            @Override public void accept(Request r) {
                Map<String,String> d = new LinkedHashMap<String,String>();
                put(d, "url", r.url());
                put(d, "error", nvl(r.failure()));
                publish("FETCH_ERROR", d);
            }
        };
        page.onRequestFailed(onReqFailed);
        detach.add(new Runnable() { @Override public void run() { page.offRequestFailed(onReqFailed); } });

        // -------------------- BiDi-Only Navigationsereignisse (nicht in Playwright onX) --------------------
        if (page instanceof PageImpl) {
            final PageImpl p = (PageImpl) page;
            final WebDriver wd = p.getWebDriver();
            final String ctxId = p.getBrowsingContextId();

            // FRAGMENT_NAVIGATED
            final Consumer<Object> onFragment = new Consumer<Object>() {
                @Override public void accept(Object ev) {
                    Map<String,String> d = new LinkedHashMap<String,String>();
                    put(d, "url", safeUrl(page));
                    publish("FRAGMENT_NAVIGATED", d);
                }
            };
            subscribe(wd, WDEventNames.FRAGMENT_NAVIGATED.getName(), ctxId, onFragment);
            detach.add(new Runnable() { @Override public void run() { wd.removeEventListener(WDEventNames.FRAGMENT_NAVIGATED.getName(), ctxId, onFragment); } });

            // HISTORY_UPDATED
            final Consumer<Object> onHistory = new Consumer<Object>() {
                @Override public void accept(Object ev) {
                    Map<String,String> d = new LinkedHashMap<String,String>();
                    put(d, "url", safeUrl(page));
                    publish("HISTORY_UPDATED", d);
                }
            };
            subscribe(wd, WDEventNames.HISTORY_UPDATED.getName(), ctxId, onHistory);
            detach.add(new Runnable() { @Override public void run() { wd.removeEventListener(WDEventNames.HISTORY_UPDATED.getName(), ctxId, onHistory); } });

            // NAVIGATION_COMMITTED
            final Consumer<Object> onCommitted = new Consumer<Object>() {
                @Override public void accept(Object ev) {
                    Map<String,String> d = new LinkedHashMap<String,String>();
                    put(d, "url", safeUrl(page));
                    publish("NAVIGATION_COMMITTED", d);
                }
            };
            subscribe(wd, WDEventNames.NAVIGATION_COMMITTED.getName(), ctxId, onCommitted);
            detach.add(new Runnable() { @Override public void run() { wd.removeEventListener(WDEventNames.NAVIGATION_COMMITTED.getName(), ctxId, onCommitted); } });

            // NAVIGATION_ABORTED
            final Consumer<Object> onAborted = new Consumer<Object>() {
                @Override public void accept(Object ev) {
                    Map<String,String> d = new LinkedHashMap<String,String>();
                    put(d, "url", safeUrl(page));
                    publish("NAVIGATION_ABORTED", d);
                }
            };
            subscribe(wd, WDEventNames.NAVIGATION_ABORTED.getName(), ctxId, onAborted);
            detach.add(new Runnable() { @Override public void run() { wd.removeEventListener(WDEventNames.NAVIGATION_ABORTED.getName(), ctxId, onAborted); } });

            // NAVIGATION_FAILED
            final Consumer<Object> onFailed = new Consumer<Object>() {
                @Override public void accept(Object ev) {
                    Map<String,String> d = new LinkedHashMap<String,String>();
                    put(d, "url", safeUrl(page));
                    publish("NAVIGATION_FAILED", d);
                }
            };
            subscribe(wd, WDEventNames.NAVIGATION_FAILED.getName(), ctxId, onFailed);
            detach.add(new Runnable() { @Override public void run() { wd.removeEventListener(WDEventNames.NAVIGATION_FAILED.getName(), ctxId, onFailed); } });
        }
    }

    /** Entfernt alle zuvor installierten Hooks (sowohl Playwright als auch BiDi). */
    public void uninstallAll() {
        for (Map.Entry<Object, List<Runnable>> e : detachActions.entrySet()) {
            for (Runnable r : e.getValue()) {
                try { r.run(); } catch (Throwable ignore) {}
            }
        }
        detachActions.clear();
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private void subscribe(final WebDriver wd, final String eventName, final String contextId, final Consumer<Object> handler) {
        WDSubscriptionRequest req = new WDSubscriptionRequest(eventName, contextId, null);
        wd.addEventListener(req, handler);
    }

    private List<Runnable> bucket(Object key) {
        List<Runnable> list = detachActions.get(key);
        if (list == null) {
            list = new ArrayList<Runnable>();
            detachActions.put(key, list);
        }
        return list;
    }

    private void publish(String kindName, Map<String,String> details) {
        // Erwartet: MetaEvent.Kind enthält die gewünschten Konstanten.
        // Wenn deine Enum anders heißt/liegt, passe dies bitte zentral an.
        MetaEvent.Kind kind = MetaEvent.Kind.valueOf(kindName);
        if (details == null || details.isEmpty()) {
            events.publish(MetaEvent.of(kind));
        } else {
            events.publish(MetaEvent.of(kind, details));
        }
    }

    private static void put(Map<String,String> m, String k, String v) {
        if (v != null) m.put(k, v);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String safeUrl(Page p) {
        try { return nvl(p.url()); } catch (Throwable t) { return ""; }
    }
}
