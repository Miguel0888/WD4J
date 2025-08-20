package de.bund.zrb.ext;

import com.google.gson.JsonObject;
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.UserContextImpl;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.support.JsonToPlaywrightMapper;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Bietet NUR die Events an, die im Mapper NICHT auf ein Playwright-Interface gemappt werden – aber auf Context-Ebene. */
public final class WDContextExtensionSupport {

    private final UserContextImpl ctx;

    public WDContextExtensionSupport(UserContextImpl ctx) {
        this.ctx = ctx;
    }

    // Listener-Maps (extern -> intern)
    private final Map<Consumer<WDBrowsingContextEvent.NavigationCommitted>, Consumer<Object>> mCommit = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.NavigationAborted>,   Consumer<Object>> mAbort  = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.FragmentNavigated>,   Consumer<Object>> mFrag   = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.HistoryUpdated>,      Consumer<Object>> mHist   = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.UserPromptClosed>,    Consumer<Object>> mPrompt = new ConcurrentHashMap<>();
    private final Map<Consumer<WDNetworkEvent.AuthRequired>,                Consumer<Object>> mAuth   = new ConcurrentHashMap<>();
    private final Map<Consumer<WDScriptEvent.RealmDestroyed>,               Consumer<Object>> mRealmD = new ConcurrentHashMap<>();
    private final Map<Consumer<WDScriptEvent.MessageWD>,                      Consumer<Object>> mMsg    = new ConcurrentHashMap<>();

    // =====================================================================
    // Generic RAW event support
    //
    // Similar to WDPageExtensionSupport, allow subscription to arbitrary
    // WebDriver BiDi events on a context level. The raw event payload is
    // forwarded without conversion, giving consumers access to the full
    // information contained in the event.

    /**
     * Map of raw event handlers keyed by event name. Each event maintains its own
     * mapping from external consumer to the internal listener registered with
     * the underlying WebDriver. This allows proper deregistration when
     * {@link #offRaw(WDEventNames, Consumer)} is called.
     */
    private final java.util.Map<WDEventNames, java.util.Map<java.util.function.Consumer<Object>, java.util.function.Consumer<Object>>> rawHandlers = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Subscribes to a raw WebDriver event on the context. Events are delivered
     * exactly as they come from the WebDriver without filtering or mapping.
     *
     * @param event   the event name to subscribe to
     * @param handler the consumer that will receive raw event objects
     */
    public void onRaw(WDEventNames event, java.util.function.Consumer<Object> handler) {
        if (event == null || handler == null) return;
        java.util.Map<java.util.function.Consumer<Object>, java.util.function.Consumer<Object>> store =
                rawHandlers.computeIfAbsent(event, k -> new java.util.concurrent.ConcurrentHashMap<>());
        if (store.containsKey(handler)) return;
        java.util.function.Consumer<Object> internal = new java.util.function.Consumer<Object>() {
            @Override public void accept(Object o) {
                handler.accept(o);
            }
        };
        // Subscribe to all contexts; the caller may ignore events not related to this context
        WDSubscriptionRequest req = new WDSubscriptionRequest(event.getName(), null, null);
        ((de.bund.zrb.BrowserImpl) ctx.browser()).getWebDriver().addEventListener(req, internal);
        store.put(handler, internal);
    }

    /**
     * Unsubscribes a previously registered raw event handler. If the handler
     * was not registered via {@link #onRaw(WDEventNames, java.util.function.Consumer)},
     * this method does nothing.
     *
     * @param event   the event name that was used for subscription
     * @param handler the original external consumer
     */
    public void offRaw(WDEventNames event, java.util.function.Consumer<Object> handler) {
        if (event == null || handler == null) return;
        java.util.Map<java.util.function.Consumer<Object>, java.util.function.Consumer<Object>> store = rawHandlers.get(event);
        if (store == null) return;
        java.util.function.Consumer<Object> internal = store.remove(handler);
        if (internal != null) {
            ((de.bund.zrb.BrowserImpl) ctx.browser()).getWebDriver().removeEventListener(event.getName(), null, internal);
        }
    }

    // ---------- Public API ----------
    public void onNavigationCommitted(Consumer<WDBrowsingContextEvent.NavigationCommitted> h) {
        subscribeTyped(WDEventNames.NAVIGATION_COMMITTED.getName(), h, mCommit, WDBrowsingContextEvent.NavigationCommitted.class);
    }
    public void offNavigationCommitted(Consumer<WDBrowsingContextEvent.NavigationCommitted> h) {
        unsubscribeTyped(WDEventNames.NAVIGATION_COMMITTED.getName(), h, mCommit);
    }

    public void onNavigationAborted(Consumer<WDBrowsingContextEvent.NavigationAborted> h) {
        subscribeTyped(WDEventNames.NAVIGATION_ABORTED.getName(), h, mAbort, WDBrowsingContextEvent.NavigationAborted.class);
    }
    public void offNavigationAborted(Consumer<WDBrowsingContextEvent.NavigationAborted> h) {
        unsubscribeTyped(WDEventNames.NAVIGATION_ABORTED.getName(), h, mAbort);
    }

    public void onFragmentNavigated(Consumer<WDBrowsingContextEvent.FragmentNavigated> h) {
        subscribeTyped(WDEventNames.FRAGMENT_NAVIGATED.getName(), h, mFrag, WDBrowsingContextEvent.FragmentNavigated.class);
    }
    public void offFragmentNavigated(Consumer<WDBrowsingContextEvent.FragmentNavigated> h) {
        unsubscribeTyped(WDEventNames.FRAGMENT_NAVIGATED.getName(), h, mFrag);
    }

    public void onHistoryUpdated(Consumer<WDBrowsingContextEvent.HistoryUpdated> h) {
        subscribeTyped(WDEventNames.HISTORY_UPDATED.getName(), h, mHist, WDBrowsingContextEvent.HistoryUpdated.class);
    }
    public void offHistoryUpdated(Consumer<WDBrowsingContextEvent.HistoryUpdated> h) {
        unsubscribeTyped(WDEventNames.HISTORY_UPDATED.getName(), h, mHist);
    }

    public void onUserPromptClosed(Consumer<WDBrowsingContextEvent.UserPromptClosed> h) {
        subscribeTyped(WDEventNames.USER_PROMPT_CLOSED.getName(), h, mPrompt, WDBrowsingContextEvent.UserPromptClosed.class);
    }
    public void offUserPromptClosed(Consumer<WDBrowsingContextEvent.UserPromptClosed> h) {
        unsubscribeTyped(WDEventNames.USER_PROMPT_CLOSED.getName(), h, mPrompt);
    }

    public void onAuthRequired(Consumer<WDNetworkEvent.AuthRequired> h) {
        subscribeTyped(WDEventNames.AUTH_REQUIRED.getName(), h, mAuth, WDNetworkEvent.AuthRequired.class);
    }
    public void offAuthRequired(Consumer<WDNetworkEvent.AuthRequired> h) {
        unsubscribeTyped(WDEventNames.AUTH_REQUIRED.getName(), h, mAuth);
    }

    public void onRealmDestroyed(Consumer<WDScriptEvent.RealmDestroyed> h) {
        subscribeTyped(WDEventNames.REALM_DESTROYED.getName(), h, mRealmD, WDScriptEvent.RealmDestroyed.class);
    }
    public void offRealmDestroyed(Consumer<WDScriptEvent.RealmDestroyed> h) {
        unsubscribeTyped(WDEventNames.REALM_DESTROYED.getName(), h, mRealmD);
    }

    public void onScriptMessage(Consumer<WDScriptEvent.MessageWD> h) {
        subscribeTyped(WDEventNames.MESSAGE.getName(), h, mMsg, WDScriptEvent.MessageWD.class);
    }
    public void offScriptMessage(Consumer<WDScriptEvent.MessageWD> h) {
        unsubscribeTyped(WDEventNames.MESSAGE.getName(), h, mMsg);
    }

    // ---------- intern ----------
    private <E> void subscribeTyped(String eventName,
                                    Consumer<E> external,
                                    Map<Consumer<E>, Consumer<Object>> store,
                                    Class<E> eventClass) {
        if (external == null) return;

        Consumer<Object> internal = new Consumer<Object>() {
            @Override public void accept(Object ev) {
                // Auf Context einschränken (falls möglich)
                String ctxId = extractContextId(ev);
                if (ctxId != null && !ctx.hasPage(ctxId)) {
                    return; // Event gehört nicht zu diesem UserContext
                }

                if (eventClass.isInstance(ev)) {
                    external.accept(eventClass.cast(ev));
                } else if (ev instanceof JsonObject) {
                    try {
                        E mapped = JsonToPlaywrightMapper.mapToInterface((JsonObject) ev, eventClass);
                        if (mapped != null) external.accept(mapped);
                    } catch (Throwable ignored) { }
                }
            }
        };

        // Context-weit abonnieren (Server schickt alle passenden; wir filtern oben)
        WDSubscriptionRequest req = new WDSubscriptionRequest(eventName, null, null);
        ((BrowserImpl) ctx.browser()).getWebDriver().addEventListener(req, internal);
        store.put(external, internal);
    }

    private String extractContextId(Object ev) {
        if (ev instanceof JsonObject) {
            JsonObject j = (JsonObject) ev;
            if (j.has("context")) return j.get("context").getAsString();
            if (j.has("source") && j.getAsJsonObject("source").has("context")) {
                return j.getAsJsonObject("source").get("context").getAsString();
            }
        }
        // Optional: weitere Typen via Reflection unterstützen (wenn deine Event-Klassen Zugriff bieten)
        return null;
    }

    private <E> void unsubscribeTyped(String eventName,
                                      Consumer<E> external,
                                      Map<Consumer<E>, Consumer<Object>> store) {
        if (external == null) return;
        Consumer<Object> internal = store.remove(external);
        if (internal != null) {
            ((BrowserImpl) ctx.browser()).getWebDriver().removeEventListener(eventName, null, internal);
        }
    }

    /** Alles deregistrieren. */
    public void detachAll() {
        mCommit.forEach((k,v) -> ((BrowserImpl) ctx.browser()).getWebDriver().removeEventListener(WDEventNames.NAVIGATION_COMMITTED.getName(), null, v));
        mAbort .forEach((k,v) -> ((BrowserImpl) ctx.browser()).getWebDriver().removeEventListener(WDEventNames.NAVIGATION_ABORTED.getName(),  null, v));
        mFrag  .forEach((k,v) -> ((BrowserImpl) ctx.browser()).getWebDriver().removeEventListener(WDEventNames.FRAGMENT_NAVIGATED.getName(),  null, v));
        mHist  .forEach((k,v) -> ((BrowserImpl) ctx.browser()).getWebDriver().removeEventListener(WDEventNames.HISTORY_UPDATED.getName(),     null, v));
        mPrompt.forEach((k,v) -> ((BrowserImpl) ctx.browser()).getWebDriver().removeEventListener(WDEventNames.USER_PROMPT_CLOSED.getName(),  null, v));
        mAuth  .forEach((k,v) -> ((BrowserImpl) ctx.browser()).getWebDriver().removeEventListener(WDEventNames.AUTH_REQUIRED.getName(),       null, v));
        mRealmD.forEach((k,v) -> ((BrowserImpl) ctx.browser()).getWebDriver().removeEventListener(WDEventNames.REALM_DESTROYED.getName(),     null, v));
        mMsg   .forEach((k,v) -> ((BrowserImpl) ctx.browser()).getWebDriver().removeEventListener(WDEventNames.MESSAGE.getName(),             null, v));

        mCommit.clear(); mAbort.clear(); mFrag.clear(); mHist.clear(); mPrompt.clear(); mAuth.clear(); mRealmD.clear(); mMsg.clear();
    }
}
