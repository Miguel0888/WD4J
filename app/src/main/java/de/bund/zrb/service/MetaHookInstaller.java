package de.bund.zrb.service;

import com.microsoft.playwright.*;
import de.bund.zrb.PageImpl;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * Wire Playwright/BiDi events for meta logging (AJAX, navigation, DOM ready, etc.).
 * Works with Page and BrowserContext (UserContextImpl). No reflection, Java 8 compatible.
 */
public final class MetaHookInstaller {

    private final List<Runnable> uninstallers = new ArrayList<Runnable>();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final MetaEventSink sink;

    /** Use a sink to route lines (console + bus). */
    public MetaHookInstaller(MetaEventSink sink) {
        this.sink = (sink != null) ? sink : new ConsoleAndBusMetaSink();
    }

    /** Install hooks on a single Page (preferred in page mode). */
    public void installOnPage(final Page page) {
        if (page == null) return;

        installCoreNetworkOnPage(page);
        installDomMilestonesOnPage(page);
        installDialogOpenedOnPage(page);
        installBiDiOnlyPageLevel(page);
    }

    /** Install hooks on a BrowserContext (used in context mode). */
    public void installOnContext(final BrowserContext ctx) {
        if (ctx == null) return;

        installCoreNetworkOnContext(ctx);

        // Auto-install on future pages:
        final Consumer<Page> onNewPage = new Consumer<Page>() {
            @Override public void accept(Page p) { installOnPage(p); }
        };
        ctx.onPage(onNewPage);
        uninstallers.add(new Runnable() { @Override public void run() { ctx.offPage(onNewPage); } });

        // Install on existing pages:
        List<Page> pages = safePages(ctx);
        for (Page p : pages) installOnPage(p);
    }

    /** Uninstall all previously installed hooks. */
    public void uninstallAll() {
        for (Runnable r : uninstallers) {
            try { r.run(); } catch (Throwable ignore) {}
        }
        uninstallers.clear();
    }

    // ---------- Page-level core ----------

    private void installCoreNetworkOnPage(final Page page) {
        final Consumer<Request> onReq = new Consumer<Request>() {
            @Override public void accept(Request r) {
                emit("AJAX_STARTED",
                        "method=" + nvl(r.method()) + ", type=" + nvl(r.resourceType()) + ", url=" + nvl(r.url()));
            }
        };
        page.onRequest(onReq);
        uninstallers.add(new Runnable() { @Override public void run() { page.offRequest(onReq); } });

        final Consumer<Response> onRespStart = new Consumer<Response>() {
            @Override public void accept(Response resp) {
                emit("AJAX_RESPONSE_STARTED", "url=" + nvl(resp.url()) + ", status=" + resp.status());
            }
        };
        page.onResponse(onRespStart);
        uninstallers.add(new Runnable() { @Override public void run() { page.offResponse(onRespStart); } });

        final Consumer<Request> onReqFinished = new Consumer<Request>() {
            @Override public void accept(Request r) {
                Response resp = r.response();
                emit("AJAX_COMPLETED",
                        "url=" + nvl(r.url()) + ", status=" + (resp != null ? resp.status() : -1));
            }
        };
        page.onRequestFinished(onReqFinished);
        uninstallers.add(new Runnable() { @Override public void run() { page.offRequestFinished(onReqFinished); } });

        final Consumer<Request> onReqFailed = new Consumer<Request>() {
            @Override public void accept(Request r) {
                emit("AJAX_FAILED", "url=" + nvl(r.url()) + ", error=" + nvl(r.failure()));
            }
        };
        page.onRequestFailed(onReqFailed);
        uninstallers.add(new Runnable() { @Override public void run() { page.offRequestFailed(onReqFailed); } });
    }

    private void installDomMilestonesOnPage(final Page page) {
        final Consumer<Page> onDom = new Consumer<Page>() {
            @Override public void accept(Page p) { emit("DOMCONTENTLOADED", null); }
        };
        page.onDOMContentLoaded(onDom);
        uninstallers.add(new Runnable() { @Override public void run() { page.offDOMContentLoaded(onDom); } });

        final Consumer<Page> onLoad = new Consumer<Page>() {
            @Override public void accept(Page p) { emit("LOAD", null); }
        };
        page.onLoad(onLoad);
        uninstallers.add(new Runnable() { @Override public void run() { page.offLoad(onLoad); } });
    }

    private void installDialogOpenedOnPage(final Page page) {
        final Consumer<Dialog> onDialog = new Consumer<Dialog>() {
            @Override public void accept(Dialog dialog) {
                emit("PROMPT_OPENED", "type=" + nvl(dialog.type()) + ", message=" + nvl(dialog.message()));
            }
        };
        page.onDialog(onDialog);
        uninstallers.add(new Runnable() { @Override public void run() { page.offDialog(onDialog); } });
    }

    // ---------- Context-level core ----------

    private void installCoreNetworkOnContext(final BrowserContext ctx) {
        final Consumer<Request> onReq = new Consumer<Request>() {
            @Override public void accept(Request r) {
                emit("AJAX_STARTED",
                        "method=" + nvl(r.method()) + ", type=" + nvl(r.resourceType()) + ", url=" + nvl(r.url()));
            }
        };
        ctx.onRequest(onReq);
        uninstallers.add(new Runnable() { @Override public void run() { ctx.offRequest(onReq); } });

        final Consumer<Response> onRespStart = new Consumer<Response>() {
            @Override public void accept(Response resp) {
                emit("AJAX_RESPONSE_STARTED", "url=" + nvl(resp.url()) + ", status=" + resp.status());
            }
        };
        ctx.onResponse(onRespStart);
        uninstallers.add(new Runnable() { @Override public void run() { ctx.offResponse(onRespStart); } });

        final Consumer<Request> onReqFinished = new Consumer<Request>() {
            @Override public void accept(Request r) {
                Response resp = r.response();
                emit("AJAX_COMPLETED",
                        "url=" + nvl(r.url()) + ", status=" + (resp != null ? resp.status() : -1));
            }
        };
        ctx.onRequestFinished(onReqFinished);
        uninstallers.add(new Runnable() { @Override public void run() { ctx.offRequestFinished(onReqFinished); } });

        final Consumer<Request> onReqFailed = new Consumer<Request>() {
            @Override public void accept(Request r) {
                emit("AJAX_FAILED", "url=" + nvl(r.url()) + ", error=" + nvl(r.failure()));
            }
        };
        ctx.onRequestFailed(onReqFailed);
        uninstallers.add(new Runnable() { @Override public void run() { ctx.offRequestFailed(onReqFailed); } });
    }

    // ---------- BiDi-only additions ----------

    private void installBiDiOnlyPageLevel(final Page page) {
        if (!(page instanceof PageImpl)) return;

        final PageImpl p = (PageImpl) page;
        final de.bund.zrb.WebDriver wd = p.getWebDriver();
        final String ctxId = p.getBrowsingContextId();

        subscribe(wd, WDEventNames.FRAGMENT_NAVIGATED.getName(), ctxId, new Consumer<Object>() {
            @Override public void accept(Object o) { emit("URL_CHANGED", "reason=fragment, url=" + nvl(page.url())); }
        });

        subscribe(wd, WDEventNames.HISTORY_UPDATED.getName(), ctxId, new Consumer<Object>() {
            @Override public void accept(Object o) { emit("URL_CHANGED", "reason=historyUpdated, url=" + nvl(page.url())); }
        });

        subscribe(wd, WDEventNames.NAVIGATION_COMMITTED.getName(), ctxId, new Consumer<Object>() {
            @Override public void accept(Object o) { emit("URL_CHANGED", "reason=committed, url=" + nvl(page.url())); }
        });

        subscribe(wd, WDEventNames.NAVIGATION_ABORTED.getName(), ctxId, new Consumer<Object>() {
            @Override public void accept(Object o) { emit("URL_CHANGED", "reason=aborted, url=" + nvl(page.url())); }
        });

        subscribe(wd, WDEventNames.USER_PROMPT_CLOSED.getName(), ctxId, new Consumer<Object>() {
            @Override public void accept(Object o) { emit("PROMPT_CLOSED", null); }
        });

        subscribe(wd, WDEventNames.AUTH_REQUIRED.getName(), ctxId, new Consumer<Object>() {
            @Override public void accept(Object o) { emit("AUTH_REQUIRED", null); }
        });
    }

    private void subscribe(final de.bund.zrb.WebDriver wd,
                           final String eventName,
                           final String contextId,
                           final Consumer<Object> handler) {
        WDSubscriptionRequest req = new WDSubscriptionRequest(eventName, contextId, null);
        wd.addEventListener(req, handler);
        uninstallers.add(new Runnable() {
            @Override public void run() { wd.removeEventListener(eventName, contextId, handler); }
        });
    }

    // ---------- Helpers ----------

    private List<Page> safePages(BrowserContext ctx) {
        try { return ctx.pages(); } catch (Throwable t) { return Collections.<Page>emptyList(); }
    }

    private void emit(String tag, String details) {
        String prefix = "[" + LocalTime.now().format(TS) + "] " + tag;
        String line = (details == null || details.trim().isEmpty()) ? prefix : (prefix + " { " + details + " }");
        sink.append(line);
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
