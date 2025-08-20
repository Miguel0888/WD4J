package de.bund.zrb.ext;

import com.google.gson.JsonObject;
import de.bund.zrb.PageImpl;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDEventNames;
import de.bund.zrb.support.JsonToPlaywrightMapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Bietet NUR die Events an, die im Mapper NICHT auf ein Playwright-Interface gemappt werden. */
public final class WDPageExtensionSupport {

    private final PageImpl page;

    public WDPageExtensionSupport(PageImpl page) {
        this.page = page;
    }

    // pro Eventtyp: extern -> intern (für off)
    private final Map<Consumer<WDBrowsingContextEvent.NavigationCommitted>, Consumer<Object>> mCommit = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.NavigationAborted>,   Consumer<Object>> mAbort  = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.FragmentNavigated>,   Consumer<Object>> mFrag   = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.HistoryUpdated>,      Consumer<Object>> mHist   = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.UserPromptClosed>,    Consumer<Object>> mPrompt = new ConcurrentHashMap<>();
    private final Map<Consumer<WDNetworkEvent.AuthRequired>,                Consumer<Object>> mAuth   = new ConcurrentHashMap<>();
    private final Map<Consumer<WDScriptEvent.RealmDestroyed>,               Consumer<Object>> mRealmD = new ConcurrentHashMap<>();

    // (Optional, da kein Playwright-Interface): script.message direkt anbieten
    private final Map<Consumer<WDScriptEvent.MessageWD>,                      Consumer<Object>> mMsg    = new ConcurrentHashMap<>();

    // ---------- Public API: on/off nur für NICHT-gemappte Fälle ----------

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

    // Kein Playwright-Interface → hier auch anbieten
    public void onScriptMessage(Consumer<WDScriptEvent.MessageWD> h) {
        subscribeTyped(WDEventNames.MESSAGE.getName(), h, mMsg, WDScriptEvent.MessageWD.class);
    }
    public void offScriptMessage(Consumer<WDScriptEvent.MessageWD> h) {
        unsubscribeTyped(WDEventNames.MESSAGE.getName(), h, mMsg);
    }

    // =====================================================================
    // Generic RAW event support
    //
    // The Playwright adapter exposes only a subset of WebDriver events via typed
    // handlers. To allow consumers to subscribe to any raw WebDriver-BiDi event
    // (including those already mapped by Playwright), generic subscribe/unsubscribe
    // methods are provided. These simply forward the raw event object to the
    // external consumer without converting it to a typed interface. Consumers
    // are responsible for interpreting the raw object.

    /**
     * Map of raw event handlers keyed by event name. Each event maintains its own
     * mapping from external consumer to the internal listener registered with
     * the underlying WebDriver. This allows proper deregistration when
     * {@link #offRaw(WDEventNames, Consumer)} is called.
     */
    private final java.util.Map<WDEventNames, java.util.Map<java.util.function.Consumer<Object>, java.util.function.Consumer<Object>>> rawHandlers = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Subscribes to a raw WebDriver event. All events produced by the underlying
     * WebDriver with the given event name will be forwarded as-is to the
     * provided consumer. Unlike the typed onXxx methods, this method does
     * not attempt to map the event to a specific interface or class; the
     * raw object (which may be a JsonObject or another DTO) is passed
     * directly. Consumers should ensure thread safety as callbacks may be
     * invoked on WebDriver threads.
     *
     * @param event the event name to subscribe to
     * @param handler the consumer that will receive raw event objects
     */
    public void onRaw(WDEventNames event, java.util.function.Consumer<Object> handler) {
        if (event == null || handler == null) return;
        java.util.Map<java.util.function.Consumer<Object>, java.util.function.Consumer<Object>> store =
                rawHandlers.computeIfAbsent(event, k -> new java.util.concurrent.ConcurrentHashMap<>());
        if (store.containsKey(handler)) return; // avoid duplicate registration
        java.util.function.Consumer<Object> internal = new java.util.function.Consumer<Object>() {
            @Override public void accept(Object o) {
                handler.accept(o);
            }
        };
        // Subscribe to the raw event; context filter is the current page's browsing context
        WDSubscriptionRequest req = new WDSubscriptionRequest(event.getName(), page.getBrowsingContextId(), null);
        page.getWebDriver().addEventListener(req, internal);
        store.put(handler, internal);
    }

    /**
     * Unsubscribes a previously registered raw WebDriver event handler. If the
     * handler was not registered via {@link #onRaw(WDEventNames, java.util.function.Consumer)},
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
            page.getWebDriver().removeEventListener(event.getName(), page.getBrowsingContextId(), internal);
        }
    }

    // ---------- intern: generisches Subscribe/Unsubscribe ----------

    private <E> void subscribeTyped(String eventName,
                                    Consumer<E> external,
                                    Map<Consumer<E>, Consumer<Object>> store,
                                    Class<E> eventClass) {
        if (external == null) return;

        Consumer<Object> internal = ev -> {
            if (eventClass.isInstance(ev)) {
                external.accept(eventClass.cast(ev));
            } else if (ev instanceof JsonObject) {
                // Fallback: Json → gewünschte Eventklasse
                try {
                    E mapped = JsonToPlaywrightMapper.mapToInterface((JsonObject) ev, eventClass);
                    if (mapped != null) external.accept(mapped);
                } catch (Throwable ignored) { /* not our type */ }
            }
        };

        WDSubscriptionRequest req = new WDSubscriptionRequest(eventName, page.getBrowsingContextId(), null);
        page.getWebDriver().addEventListener(req, internal);
        store.put(external, internal);
    }

    private <E> void unsubscribeTyped(String eventName,
                                      Consumer<E> external,
                                      Map<Consumer<E>, Consumer<Object>> store) {
        if (external == null) return;
        Consumer<Object> internal = store.remove(external);
        if (internal != null) {
            page.getWebDriver().removeEventListener(eventName, page.getBrowsingContextId(), internal);
        }
    }

    /** Alles deregistrieren. */
    public void detachAll() {
        mCommit.forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.NAVIGATION_COMMITTED.getName(), page.getBrowsingContextId(), v));
        mAbort .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.NAVIGATION_ABORTED.getName(),  page.getBrowsingContextId(), v));
        mFrag  .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.FRAGMENT_NAVIGATED.getName(),  page.getBrowsingContextId(), v));
        mHist  .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.HISTORY_UPDATED.getName(),     page.getBrowsingContextId(), v));
        mPrompt.forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.USER_PROMPT_CLOSED.getName(),  page.getBrowsingContextId(), v));
        mAuth  .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.AUTH_REQUIRED.getName(),       page.getBrowsingContextId(), v));
        mRealmD.forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.REALM_DESTROYED.getName(),     page.getBrowsingContextId(), v));
        mMsg   .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.MESSAGE.getName(),             page.getBrowsingContextId(), v));

        mCommit.clear(); mAbort.clear(); mFrag.clear(); mHist.clear(); mPrompt.clear(); mAuth.clear(); mRealmD.clear(); mMsg.clear();
    }
}
