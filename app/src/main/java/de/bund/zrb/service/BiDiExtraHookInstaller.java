package de.bund.zrb.service;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import de.bund.zrb.PageImpl;
import de.bund.zrb.WebDriver;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Bridges for WebDriver BiDi events that Playwright does NOT expose as onX/offX.
 * API intentionally mirrors Playwright's on/off style per Page.
 */
public final class BiDiExtraHookInstaller {

    // key = eventName + "@" + identityHash(page)
    private final Map<String, Map<Consumer<?>, Consumer<Object>>> wires = new ConcurrentHashMap<String, Map<Consumer<?>, Consumer<Object>>>();

    // ---------- public on/off API (Page-scoped) ----------

    public void onFragmentNavigated(Page page, Consumer<Page> handler) {
        subscribePageEvent(page, WDEventNames.FRAGMENT_NAVIGATED.getName(), handler, new Consumer<Object>() {
            @Override public void accept(Object ignored) { handler.accept(page); }
        });
    }

    public void offFragmentNavigated(Page page, Consumer<Page> handler) {
        unsubscribePageEvent(page, WDEventNames.FRAGMENT_NAVIGATED.getName(), handler);
    }

    public void onHistoryUpdated(Page page, Consumer<Page> handler) {
        subscribePageEvent(page, WDEventNames.HISTORY_UPDATED.getName(), handler, new Consumer<Object>() {
            @Override public void accept(Object ignored) { handler.accept(page); }
        });
    }

    public void offHistoryUpdated(Page page, Consumer<Page> handler) {
        unsubscribePageEvent(page, WDEventNames.HISTORY_UPDATED.getName(), handler);
    }

    public void onNavigationCommitted(Page page, Consumer<Page> handler) {
        subscribePageEvent(page, WDEventNames.NAVIGATION_COMMITTED.getName(), handler, new Consumer<Object>() {
            @Override public void accept(Object ignored) { handler.accept(page); }
        });
    }

    public void offNavigationCommitted(Page page, Consumer<Page> handler) {
        unsubscribePageEvent(page, WDEventNames.NAVIGATION_COMMITTED.getName(), handler);
    }

    public void onNavigationAborted(Page page, Consumer<Page> handler) {
        subscribePageEvent(page, WDEventNames.NAVIGATION_ABORTED.getName(), handler, new Consumer<Object>() {
            @Override public void accept(Object ignored) { handler.accept(page); }
        });
    }

    public void offNavigationAborted(Page page, Consumer<Page> handler) {
        unsubscribePageEvent(page, WDEventNames.NAVIGATION_ABORTED.getName(), handler);
    }

    /** BiDi network.authRequired (Playwright hat dafür kein onX). */
    public void onAuthRequired(Page page, Consumer<Request> handler) {
        subscribePageEvent(page, WDEventNames.AUTH_REQUIRED.getName(), handler, new Consumer<Object>() {
            @Override public void accept(Object ev) {
                if (ev instanceof Request) handler.accept((Request) ev);
                // Wenn der Mapper mal nicht greift, ignorieren wir stumm statt ClassCast
            }
        });
    }

    public void offAuthRequired(Page page, Consumer<Request> handler) {
        unsubscribePageEvent(page, WDEventNames.AUTH_REQUIRED.getName(), handler);
    }

    // ---------- core wiring helpers ----------

    private <T> void subscribePageEvent(Page page,
                                        String eventName,
                                        Consumer<T> userHandler,
                                        Consumer<Object> wireHandler) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(userHandler, "handler");

        if (!(page instanceof PageImpl)) {
            throw new IllegalArgumentException("BiDiExtraHookInstaller requires PageImpl");
        }
        final PageImpl p = (PageImpl) page;
        final WebDriver wd = p.getWebDriver();
        final String ctxId = p.getBrowsingContextId();

        // store wrapper so we can remove exactly the same instance
        Map<Consumer<?>, Consumer<Object>> byHandler = wires.computeIfAbsent(key(eventName, page), k -> new ConcurrentHashMap<Consumer<?>, Consumer<Object>>());
        byHandler.put(userHandler, wireHandler);

        WDSubscriptionRequest req = new WDSubscriptionRequest(eventName, ctxId, null);
        wd.addEventListener(req, wireHandler);
    }

    private <T> void unsubscribePageEvent(Page page, String eventName, Consumer<T> userHandler) {
        Objects.requireNonNull(page, "page");
        Objects.requireNonNull(userHandler, "handler");

        if (!(page instanceof PageImpl)) return; // nothing to do
        final PageImpl p = (PageImpl) page;
        final WebDriver wd = p.getWebDriver();
        final String ctxId = p.getBrowsingContextId();

        Map<Consumer<?>, Consumer<Object>> byHandler = wires.get(key(eventName, page));
        if (byHandler == null) return;

        @SuppressWarnings("unchecked")
        Consumer<Object> wire = byHandler.remove(userHandler);
        if (wire != null) {
            wd.removeEventListener(eventName, ctxId, wire);
        }

        if (byHandler.isEmpty()) {
            wires.remove(key(eventName, page));
        }
    }

    private static String key(String eventName, Page page) {
        return eventName + "@" + System.identityHashCode(page);
    }
}
