package de.bund.zrb.service;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.Frame;
import de.bund.zrb.meta.MetaEvent;
import de.bund.zrb.meta.MetaEventService;
import de.bund.zrb.meta.MetaEventServiceImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Install and remove Playwright onX/offX hooks to publish MetaEvents for the UI drawer. */
public final class MetaHookInstaller {

    private final MetaEventService events;

    // Keep strong references to listeners so offX can remove them reliably
    private final Map<Object, List<Runnable>> detachActions = new ConcurrentHashMap<Object, List<Runnable>>();

    public MetaHookInstaller() {
        this(MetaEventServiceImpl.getInstance());
    }

    public MetaHookInstaller(MetaEventService events) {
        this.events = events;
    }

    /** Install hooks on a BrowserContext (network + page lifecycle) and on all current pages. */
    public void installOnContext(final BrowserContext ctx) {
        if (ctx == null) return;

        // Track detach actions for this context
        final List<Runnable> detach = getBucket(ctx);

        // Network events (context-wide)
        final Consumer<Request> onReq = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String, String> d = new HashMap<String, String>();
                d.put("url", r.url());
                if (r.method() != null) d.put("method", r.method());
                if (r.resourceType() != null) d.put("type", r.resourceType());
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_STARTED, d));
            }
        };
        ctx.onRequest(onReq);
        detach.add(new Runnable() { public void run() { ctx.offRequest(onReq); } });

        final Consumer<Response> onResp = new Consumer<Response>() {
            public void accept(Response r) {
                Map<String, String> d = new HashMap<String, String>();
                d.put("url", r.url());
                d.put("status", String.valueOf(r.status()));
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_COMPLETED, d));
            }
        };
        ctx.onResponse(onResp);
        detach.add(new Runnable() { public void run() { ctx.offResponse(onResp); } });

        final Consumer<Request> onReqFailed = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String, String> d = new HashMap<String, String>();
                d.put("url", r.url());
                d.put("status", "FAILED");
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_COMPLETED, d));
            }
        };
        ctx.onRequestFailed(onReqFailed);
        detach.add(new Runnable() { public void run() { ctx.offRequestFailed(onReqFailed); } });

        // Already open pages
        List<Page> pages = ctx.pages();
        if (pages != null) {
            for (Page p : pages) {
                installOnPage(p);
            }
        }

        // New pages
        final Consumer<Page> onPage = new Consumer<Page>() {
            public void accept(Page page) { installOnPage(page); }
        };
        ctx.onPage(onPage);
        detach.add(new Runnable() { public void run() { ctx.offPage(onPage); } });
    }

    /** Install hooks on a single Page (load states, frame navigation, optional page-level network). */
    public void installOnPage(final Page page) {
        if (page == null) return;

        final List<Runnable> detach = getBucket(page);

        // LOAD
        final Consumer<Page> onLoad = new Consumer<Page>() {
            public void accept(Page p) {
                events.publish(MetaEvent.of(MetaEvent.Kind.LOAD));
            }
        };
        page.onLoad(onLoad);
        detach.add(new Runnable() { public void run() { page.offLoad(onLoad); } });

        // DOMContentLoaded
        final Consumer<Page> onDom = new Consumer<Page>() {
            public void accept(Page p) {
                events.publish(MetaEvent.of(MetaEvent.Kind.DOMCONTENTLOADED));
            }
        };
        page.onDOMContentLoaded(onDom);
        detach.add(new Runnable() { public void run() { page.offDOMContentLoaded(onDom); } });

        // Main-frame navigations → URL_CHANGED
        final Consumer<Frame> onFrameNav = new Consumer<Frame>() {
            public void accept(Frame f) {
                try {
                    if (f == page.mainFrame()) {
                        Map<String, String> d = new HashMap<String, String>();
                        d.put("url", page.url());
                        events.publish(MetaEvent.of(MetaEvent.Kind.URL_CHANGED, d));
                    }
                } catch (Throwable ignore) { /* keep robust */ }
            }
        };
        page.onFrameNavigated(onFrameNav);
        detach.add(new Runnable() { public void run() { page.offFrameNavigated(onFrameNav); } });

        // Optional: page-level network (often redundant to context-level, but useful per-page)
        final Consumer<Request> onReq = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String, String> d = new HashMap<String, String>();
                d.put("url", r.url());
                if (r.method() != null) d.put("method", r.method());
                if (r.resourceType() != null) d.put("type", r.resourceType());
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_STARTED, d));
            }
        };
        page.onRequest(onReq);
        detach.add(new Runnable() { public void run() { page.offRequest(onReq); } });

        final Consumer<Response> onResp = new Consumer<Response>() {
            public void accept(Response r) {
                Map<String, String> d = new HashMap<String, String>();
                d.put("url", r.url());
                d.put("status", String.valueOf(r.status()));
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_COMPLETED, d));
            }
        };
        page.onResponse(onResp);
        detach.add(new Runnable() { public void run() { page.offResponse(onResp); } });

        final Consumer<Request> onReqFailed = new Consumer<Request>() {
            public void accept(Request r) {
                Map<String, String> d = new HashMap<String, String>();
                d.put("url", r.url());
                d.put("status", "FAILED");
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_COMPLETED, d));
            }
        };
        page.onRequestFailed(onReqFailed);
        detach.add(new Runnable() { public void run() { page.offRequestFailed(onReqFailed); } });
    }

    /** Remove all hooks that were installed via this instance. */
    public void uninstallAll() {
        for (Map.Entry<Object, List<Runnable>> e : detachActions.entrySet()) {
            List<Runnable> actions = e.getValue();
            for (Runnable r : actions) {
                try { r.run(); } catch (Throwable ignore) { }
            }
        }
        detachActions.clear();
    }

    // ---------- helpers ----------

    private List<Runnable> getBucket(Object key) {
        List<Runnable> list = detachActions.get(key);
        if (list == null) {
            list = new ArrayList<Runnable>();
            detachActions.put(key, list);
        }
        return list;
    }
}
