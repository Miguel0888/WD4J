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
    private final BrowserImpl browser; // für getWebDriver()

    public WDContextExtensionSupport(UserContextImpl ctx) {
        this.ctx = ctx;
        this.browser = (BrowserImpl) ctx.browser();
    }

    // Listener-Maps (extern -> intern)
    private final Map<Consumer<WDBrowsingContextEvent.NavigationCommitted>, Consumer<Object>> mCommit = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.NavigationAborted>,   Consumer<Object>> mAbort  = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.FragmentNavigated>,   Consumer<Object>> mFrag   = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.HistoryUpdated>,      Consumer<Object>> mHist   = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.UserPromptClosed>,    Consumer<Object>> mPrompt = new ConcurrentHashMap<>();
    private final Map<Consumer<WDNetworkEvent.AuthRequired>,                Consumer<Object>> mAuth   = new ConcurrentHashMap<>();
    private final Map<Consumer<WDScriptEvent.RealmDestroyed>,               Consumer<Object>> mRealmD = new ConcurrentHashMap<>();
    private final Map<Consumer<WDScriptEvent.Message>,                      Consumer<Object>> mMsg    = new ConcurrentHashMap<>();

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

    public void onScriptMessage(Consumer<WDScriptEvent.Message> h) {
        subscribeTyped(WDEventNames.MESSAGE.getName(), h, mMsg, WDScriptEvent.Message.class);
    }
    public void offScriptMessage(Consumer<WDScriptEvent.Message> h) {
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
        browser.getWebDriver().addEventListener(req, internal);
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
            browser.getWebDriver().removeEventListener(eventName, null, internal);
        }
    }

    /** Alles deregistrieren. */
    public void detachAll() {
        mCommit.forEach((k,v) -> browser.getWebDriver().removeEventListener(WDEventNames.NAVIGATION_COMMITTED.getName(), null, v));
        mAbort .forEach((k,v) -> browser.getWebDriver().removeEventListener(WDEventNames.NAVIGATION_ABORTED.getName(),  null, v));
        mFrag  .forEach((k,v) -> browser.getWebDriver().removeEventListener(WDEventNames.FRAGMENT_NAVIGATED.getName(),  null, v));
        mHist  .forEach((k,v) -> browser.getWebDriver().removeEventListener(WDEventNames.HISTORY_UPDATED.getName(),     null, v));
        mPrompt.forEach((k,v) -> browser.getWebDriver().removeEventListener(WDEventNames.USER_PROMPT_CLOSED.getName(),  null, v));
        mAuth  .forEach((k,v) -> browser.getWebDriver().removeEventListener(WDEventNames.AUTH_REQUIRED.getName(),       null, v));
        mRealmD.forEach((k,v) -> browser.getWebDriver().removeEventListener(WDEventNames.REALM_DESTROYED.getName(),     null, v));
        mMsg   .forEach((k,v) -> browser.getWebDriver().removeEventListener(WDEventNames.MESSAGE.getName(),             null, v));

        mCommit.clear(); mAbort.clear(); mFrag.clear(); mHist.clear(); mPrompt.clear(); mAuth.clear(); mRealmD.clear(); mMsg.clear();
    }
}
