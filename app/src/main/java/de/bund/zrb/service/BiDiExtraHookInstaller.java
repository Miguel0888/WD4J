package de.bund.zrb.service;

import com.google.gson.JsonObject;
import com.microsoft.playwright.Page;
import de.bund.zrb.WebDriver;
import de.bund.zrb.meta.MetaEvent;
import de.bund.zrb.meta.MetaEventService;
import de.bund.zrb.meta.MetaEventServiceImpl;
import de.bund.zrb.websocket.WDEventNames;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.support.JsonToPlaywrightMapper;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDNetworkEvent;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/** Subscribe to the missing BiDi events (per in 'WDEventNames' matrix) and publish them as MetaEvents. */
public final class BiDiExtraHookInstaller {

    private final MetaEventService events = MetaEventServiceImpl.getInstance();
    private final List<Runnable> detach = new CopyOnWriteArrayList<Runnable>();

    public void installForPage(Page page) {
        // Support only our PageImpl to access WebDriver and context id
        if (!(page instanceof de.bund.zrb.PageImpl)) return;

        final de.bund.zrb.PageImpl p = (de.bund.zrb.PageImpl) page;
        final WebDriver wd = p.getWebDriver();
        final String ctx = p.getBrowsingContextId();

        // --- BrowsingContext: fragmentNavigated ---
        subscribe(wd, WDEventNames.FRAGMENT_NAVIGATED.getName(), ctx, new Consumer<JsonObject>() {
            public void accept(JsonObject json) {
                WDBrowsingContextEvent.FragmentNavigated ev =
                        JsonToPlaywrightMapper.mapToInterface(json, WDBrowsingContextEvent.FragmentNavigated.class);
                Map<String,String> d = new HashMap<String,String>();
                safePut(d, "phase", "fragmentNavigated");
                safePut(d, "url", ev.getParams().getUrl());
                events.publish(MetaEvent.of(MetaEvent.Kind.URL_CHANGED, d));
            }
        });

        // --- BrowsingContext: historyUpdated ---
        subscribe(wd, WDEventNames.HISTORY_UPDATED.getName(), ctx, new Consumer<JsonObject>() {
            public void accept(JsonObject json) {
                WDBrowsingContextEvent.HistoryUpdated ev =
                        JsonToPlaywrightMapper.mapToInterface(json, WDBrowsingContextEvent.HistoryUpdated.class);
                Map<String,String> d = new HashMap<String,String>();
                safePut(d, "phase", "historyUpdated");
                safePut(d, "url", ev.getParams().getUrl());
                events.publish(MetaEvent.of(MetaEvent.Kind.URL_CHANGED, d));
            }
        });

        // --- BrowsingContext: navigationAborted ---
        subscribe(wd, WDEventNames.NAVIGATION_ABORTED.getName(), ctx, new Consumer<JsonObject>() {
            public void accept(JsonObject json) {
                WDBrowsingContextEvent.NavigationAborted ev =
                        JsonToPlaywrightMapper.mapToInterface(json, WDBrowsingContextEvent.NavigationAborted.class);
                Map<String,String> d = new HashMap<String,String>();
                safePut(d, "phase", "aborted");
                safePut(d, "url", ev.getParams().getUrl());
                events.publish(MetaEvent.of(MetaEvent.Kind.URL_CHANGED, d));
            }
        });

        // --- BrowsingContext: navigationCommitted ---
        subscribe(wd, WDEventNames.NAVIGATION_COMMITTED.getName(), ctx, new Consumer<JsonObject>() {
            public void accept(JsonObject json) {
                WDBrowsingContextEvent.NavigationCommitted ev =
                        JsonToPlaywrightMapper.mapToInterface(json, WDBrowsingContextEvent.NavigationCommitted.class);
                Map<String,String> d = new HashMap<String,String>();
                safePut(d, "phase", "committed");
                safePut(d, "url", ev.getParams().getUrl());
                events.publish(MetaEvent.of(MetaEvent.Kind.URL_CHANGED, d));
            }
        });

        // --- BrowsingContext: userPromptClosed ---
        subscribe(wd, WDEventNames.USER_PROMPT_CLOSED.getName(), ctx, new Consumer<JsonObject>() {
            public void accept(JsonObject json) {
                WDBrowsingContextEvent.UserPromptClosed ev =
                        JsonToPlaywrightMapper.mapToInterface(json, WDBrowsingContextEvent.UserPromptClosed.class);
                Map<String,String> d = new HashMap<String,String>();
                d.put("userPrompt", "closed");
                // Use a neutral meta kind that is already present; avoid adding new kinds
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_COMPLETED, d));
            }
        });

        // --- Network: authRequired ---
        subscribe(wd, WDEventNames.AUTH_REQUIRED.getName(), ctx, new Consumer<JsonObject>() {
            public void accept(JsonObject json) {
                WDNetworkEvent.AuthRequired ev =
                        JsonToPlaywrightMapper.mapToInterface(json, WDNetworkEvent.AuthRequired.class);
                Map<String,String> d = new HashMap<String,String>();
                d.put("auth", "required");
                // If request is present, add URL/method for context
                try {
                    if (ev.getParams() != null && ev.getParams().getRequest() != null) {
                        safePut(d, "url", ev.getParams().getRequest().getUrl());
                        safePut(d, "method", ev.getParams().getRequest().getMethod());
                    }
                } catch (Throwable ignore) { }
                events.publish(MetaEvent.of(MetaEvent.Kind.AJAX_STARTED, d));
            }
        });
    }

    public void uninstallAll() {
        for (Runnable r : detach) {
            try { r.run(); } catch (Throwable ignore) { }
        }
        detach.clear();
    }

    // ---------- intern ----------

    private void subscribe(final WebDriver wd, final String event, final String ctx, final Consumer<JsonObject> handler) {
        final WDSubscriptionRequest req = new WDSubscriptionRequest(event, ctx, null);
        wd.addEventListener(req, handler);
        // Ensure we remove the exact same handler instance
        detach.add(new Runnable() { public void run() { wd.removeEventListener(event, ctx, handler); } });
    }

    private static void safePut(Map<String, String> d, String k, String v) {
        if (v != null && v.length() > 0) d.put(k, v);
    }
}
