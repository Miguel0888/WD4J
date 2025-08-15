package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import de.bund.zrb.PageImpl;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Install extra BiDi hooks on Page or BrowserContext to surface meta events.
 * Keep handlers so they can be properly removed.
 */
public final class MetaHookInstaller {

    private final List<Runnable> uninstallers = new ArrayList<Runnable>();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS", Locale.ROOT);

    // ---------- Public API ----------

    /** Install hooks on a single Page (preferred in page mode). */
    public void installOnPage(final Page page) {
        if (page == null) return;

        installCoreNetworkOnPage(page);
        installDomMilestonesOnPage(page);
        installDialogOpenedOnPage(page);
        installBiDiOnlyPageLevel(page); // fragment/history/commit/abort/promptClosed/authRequired
    }

    /** Install hooks on a BrowserContext (used in context mode). */
    public void installOnContext(final BrowserContext ctx) {
        if (ctx == null) return;

        installCoreNetworkOnContext(ctx);
        // DOM milestones, navigation and prompt events are page-scoped; install them in page mode.
    }

    /** Uninstall all previously installed hooks. */
    public void uninstallAll() {
        for (Runnable r : uninstallers) {
            try { r.run(); } catch (Throwable ignore) {}
        }
        uninstallers.clear();
    }

    // ---------- Page-level core (Playwright onX/offX) ----------

    private void installCoreNetworkOnPage(final Page page) {
        final Consumer<Request> onReq = new Consumer<Request>() {
            @Override public void accept(Request r) {
                log("AJAX_STARTED",
                        "method=" + nvl(r.method()) +
                                ", type=" + nvl(r.resourceType()) +
                                ", url=" + nvl(r.url()));
            }
        };
        page.onRequest(onReq);
        uninstallers.add(new Runnable() { @Override public void run() { page.offRequest(onReq); } });

        final Consumer<Response> onRespStart = new Consumer<Response>() {
            @Override public void accept(Response resp) {
                log("AJAX_RESPONSE_STARTED",
                        "url=" + nvl(resp.url()) +
                                ", status=" + resp.status());
            }
        };
        page.onResponse(onRespStart);
        uninstallers.add(new Runnable() { @Override public void run() { page.offResponse(onRespStart); } });

        final Consumer<Request> onReqFinished = new Consumer<Request>() {
            @Override public void accept(Request r) {
                Response resp = r.response();
                log("AJAX_COMPLETED",
                        "url=" + nvl(r.url()) +
                                ", status=" + (resp != null ? resp.status() : -1));
            }
        };
        page.onRequestFinished(onReqFinished);
        uninstallers.add(new Runnable() { @Override public void run() { page.offRequestFinished(onReqFinished); } });

        final Consumer<Request> onReqFailed = new Consumer<Request>() {
            @Override public void accept(Request r) {
                log("AJAX_FAILED",
                        "url=" + nvl(r.url()) +
                                ", error=" + nvl(r.failure()));
            }
        };
        page.onRequestFailed(onReqFailed);
        uninstallers.add(new Runnable() { @Override public void run() { page.offRequestFailed(onReqFailed); } });
    }

    private void installDomMilestonesOnPage(final Page page) {
        final Consumer<Page> onDom = new Consumer<Page>() {
            @Override public void accept(Page p) { log("DOMCONTENTLOADED", null); }
        };
        page.onDOMContentLoaded(onDom);
        uninstallers.add(new Runnable() { @Override public void run() { page.offDOMContentLoaded(onDom); } });

        final Consumer<Page> onLoad = new Consumer<Page>() {
            @Override public void accept(Page p) { log("LOAD", null); }
        };
        page.onLoad(onLoad);
        uninstallers.add(new Runnable() { @Override public void run() { page.offLoad(onLoad); } });
    }

    private void installDialogOpenedOnPage(final Page page) {
        final Consumer<com.microsoft.playwright.Dialog> onDialog = new Consumer<com.microsoft.playwright.Dialog>() {
            @Override public void accept(com.microsoft.playwright.Dialog dialog) {
                log("PROMPT_OPENED", "type=" + nvl(dialog.type()) + ", message=" + nvl(dialog.message()));
            }
        };
        page.onDialog(onDialog);
        uninstallers.add(new Runnable() { @Override public void run() { page.offDialog(onDialog); } });
    }

    // ---------- Context-level core (Playwright onX/offX) ----------

    private void installCoreNetworkOnContext(final BrowserContext ctx) {
        final Consumer<Request> onReq = new Consumer<Request>() {
            @Override public void accept(Request r) {
                log("AJAX_STARTED",
                        "method=" + nvl(r.method()) +
                                ", type=" + nvl(r.resourceType()) +
                                ", url=" + nvl(r.url()));
            }
        };
        ctx.onRequest(onReq);
        uninstallers.add(new Runnable() { @Override public void run() { ctx.offRequest(onReq); } });

        final Consumer<Response> onRespStart = new Consumer<Response>() {
            @Override public void accept(Response resp) {
                log("AJAX_RESPONSE_STARTED",
                        "url=" + nvl(resp.url()) +
                                ", status=" + resp.status());
            }
        };
        ctx.onResponse(onRespStart);
        uninstallers.add(new Runnable() { @Override public void run() { ctx.offResponse(onRespStart); } });

        final Consumer<Request> onReqFinished = new Consumer<Request>() {
            @Override public void accept(Request r) {
                Response resp = r.response();
                log("AJAX_COMPLETED",
                        "url=" + nvl(r.url()) +
                                ", status=" + (resp != null ? resp.status() : -1));
            }
        };
        ctx.onRequestFinished(onReqFinished);
        uninstallers.add(new Runnable() { @Override public void run() { ctx.offRequestFinished(onReqFinished); } });

        final Consumer<Request> onReqFailed = new Consumer<Request>() {
            @Override public void accept(Request r) {
                log("AJAX_FAILED",
                        "url=" + nvl(r.url()) +
                                ", error=" + nvl(r.failure()));
            }
        };
        ctx.onRequestFailed(onReqFailed);
        uninstallers.add(new Runnable() { @Override public void run() { ctx.offRequestFailed(onReqFailed); } });
    }

    // ---------- BiDi-only additions (not all are exposed as Page onX) ----------

    private void installBiDiOnlyPageLevel(final Page page) {
        if (!(page instanceof PageImpl)) return;

        final PageImpl p = (PageImpl) page;
        final de.bund.zrb.WebDriver wd = p.getWebDriver();
        final String ctxId = p.getBrowsingContextId();

        // Use generic Consumer<Object> because mapper maps to different wrapper types internally.
        final Consumer<Object> onFragment = new Consumer<Object>() {
            @Override public void accept(Object ev) {
                log("URL_CHANGED", "reason=fragment, url=" + nvl(page.url()));
            }
        };
        subscribe(wd, WDEventNames.FRAGMENT_NAVIGATED.getName(), ctxId, onFragment);

        final Consumer<Object> onHistory = new Consumer<Object>() {
            @Override public void accept(Object ev) {
                log("URL_CHANGED", "reason=historyUpdated, url=" + nvl(page.url()));
            }
        };
        subscribe(wd, WDEventNames.HISTORY_UPDATED.getName(), ctxId, onHistory);

        final Consumer<Object> onCommit = new Consumer<Object>() {
            @Override public void accept(Object ev) {
                log("URL_CHANGED", "reason=committed, url=" + nvl(page.url()));
            }
        };
        subscribe(wd, WDEventNames.NAVIGATION_COMMITTED.getName(), ctxId, onCommit);

        final Consumer<Object> onAbort = new Consumer<Object>() {
            @Override public void accept(Object ev) {
                log("URL_CHANGED", "reason=aborted, url=" + nvl(page.url()));
            }
        };
        subscribe(wd, WDEventNames.NAVIGATION_ABORTED.getName(), ctxId, onAbort);

        final Consumer<Object> onPromptClosed = new Consumer<Object>() {
            @Override public void accept(Object ev) {
                log("PROMPT_CLOSED", null);
            }
        };
        subscribe(wd, WDEventNames.USER_PROMPT_CLOSED.getName(), ctxId, onPromptClosed);

        final Consumer<Object> onAuth = new Consumer<Object>() {
            @Override public void accept(Object ev) {
                if (ev instanceof Request) {
                    Request r = (Request) ev;
                    log("AUTH_REQUIRED", "url=" + nvl(r.url()) + ", method=" + nvl(r.method()));
                } else {
                    log("AUTH_REQUIRED", null);
                }
            }
        };
        subscribe(wd, WDEventNames.AUTH_REQUIRED.getName(), ctxId, onAuth);
    }

    private void subscribe(final de.bund.zrb.WebDriver wd,
                           final String eventName,
                           final String contextId,
                           final Consumer<Object> handler) {
        WDSubscriptionRequest req = new WDSubscriptionRequest(eventName, contextId, null);
        wd.addEventListener(req, handler);
        uninstallers.add(new Runnable() {
            @Override public void run() {
                wd.removeEventListener(eventName, contextId, handler);
            }
        });
    }

    // ---------- Logging helpers ----------

    private void log(String tag, String details) {
        String prefix = "[" + LocalTime.now().format(TS) + "] " + tag;
        if (details == null || details.trim().isEmpty()) {
            System.out.println(prefix);
        } else {
            System.out.println(prefix + " { " + details + " }");
        }
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
