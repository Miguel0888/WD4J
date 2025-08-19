package de.bund.zrb.ext;

import com.google.gson.JsonObject;
import de.bund.zrb.PageImpl;
import de.bund.zrb.support.JsonToPlaywrightMapper;
import de.bund.zrb.websocket.WDEventNames;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.event.WDLogEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Hält alle Mappings und macht das eigentliche subscribe/off. */
public final class WDPageExtensionSupport {

    private final PageImpl page;

    public WDPageExtensionSupport(PageImpl page) {
        this.page  = page;
    }

    // pro Eventtyp: extern -> intern (für off)
    private final Map<Consumer<WDNetworkEvent.BeforeRequestSent>,   Consumer<Object>> mNetBefore = new ConcurrentHashMap<>();
    private final Map<Consumer<WDNetworkEvent.ResponseStarted>,     Consumer<Object>> mNetStart  = new ConcurrentHashMap<>();
    private final Map<Consumer<WDNetworkEvent.ResponseCompleted>,   Consumer<Object>> mNetDone   = new ConcurrentHashMap<>();
    private final Map<Consumer<WDNetworkEvent.FetchError>,          Consumer<Object>> mNetErr    = new ConcurrentHashMap<>();

    private final Map<Consumer<WDBrowsingContextEvent.NavigationStarted>,   Consumer<Object>> mNavStart = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.FragmentNavigated>,   Consumer<Object>> mFrag     = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.HistoryUpdated>,      Consumer<Object>> mHist     = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.NavigationCommitted>, Consumer<Object>> mCommit   = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.NavigationAborted>,   Consumer<Object>> mAbort    = new ConcurrentHashMap<>();
    private final Map<Consumer<WDBrowsingContextEvent.NavigationFailed>,    Consumer<Object>> mFail     = new ConcurrentHashMap<>();

    private final Map<Consumer<WDScriptEvent.Message>, Consumer<Object>> mScript = new ConcurrentHashMap<>();
    private final Map<Consumer<WDLogEvent.EntryAdded>, Consumer<Object>> mLog    = new ConcurrentHashMap<>();

    // ---------- Public API (ruft unten die generische subscribe/unsubscribe auf) ----------

    // Network
    public void onBeforeRequestSent(Consumer<WDNetworkEvent.BeforeRequestSent> h) { subscribeTyped(WDEventNames.BEFORE_REQUEST_SENT.getName(), h, mNetBefore, WDNetworkEvent.BeforeRequestSent.class); }
    public void offBeforeRequestSent(Consumer<WDNetworkEvent.BeforeRequestSent> h){ unsubscribeTyped(WDEventNames.BEFORE_REQUEST_SENT.getName(), h, mNetBefore); }

    public void onResponseStarted(Consumer<WDNetworkEvent.ResponseStarted> h)     { subscribeTyped(WDEventNames.RESPONSE_STARTED.getName(), h, mNetStart,  WDNetworkEvent.ResponseStarted.class); }
    public void offResponseStarted(Consumer<WDNetworkEvent.ResponseStarted> h)    { unsubscribeTyped(WDEventNames.RESPONSE_STARTED.getName(), h, mNetStart); }

    public void onResponseCompleted(Consumer<WDNetworkEvent.ResponseCompleted> h) { subscribeTyped(WDEventNames.RESPONSE_COMPLETED.getName(), h, mNetDone,  WDNetworkEvent.ResponseCompleted.class); }
    public void offResponseCompleted(Consumer<WDNetworkEvent.ResponseCompleted> h){ unsubscribeTyped(WDEventNames.RESPONSE_COMPLETED.getName(), h, mNetDone); }

    public void onFetchError(Consumer<WDNetworkEvent.FetchError> h)               { subscribeTyped(WDEventNames.FETCH_ERROR.getName(), h, mNetErr,   WDNetworkEvent.FetchError.class); }
    public void offFetchError(Consumer<WDNetworkEvent.FetchError> h)              { unsubscribeTyped(WDEventNames.FETCH_ERROR.getName(), h, mNetErr); }

    // BrowsingContext
    public void onNavigationStarted(Consumer<WDBrowsingContextEvent.NavigationStarted> h)   { subscribeTyped(WDEventNames.NAVIGATION_STARTED.getName(),   h, mNavStart, WDBrowsingContextEvent.NavigationStarted.class); }
    public void offNavigationStarted(Consumer<WDBrowsingContextEvent.NavigationStarted> h)  { unsubscribeTyped(WDEventNames.NAVIGATION_STARTED.getName(),  h, mNavStart); }

    public void onFragmentNavigated(Consumer<WDBrowsingContextEvent.FragmentNavigated> h)   { subscribeTyped(WDEventNames.FRAGMENT_NAVIGATED.getName(),    h, mFrag,     WDBrowsingContextEvent.FragmentNavigated.class); }
    public void offFragmentNavigated(Consumer<WDBrowsingContextEvent.FragmentNavigated> h)  { unsubscribeTyped(WDEventNames.FRAGMENT_NAVIGATED.getName(),   h, mFrag); }

    public void onHistoryUpdated(Consumer<WDBrowsingContextEvent.HistoryUpdated> h)         { subscribeTyped(WDEventNames.HISTORY_UPDATED.getName(),       h, mHist,     WDBrowsingContextEvent.HistoryUpdated.class); }
    public void offHistoryUpdated(Consumer<WDBrowsingContextEvent.HistoryUpdated> h)        { unsubscribeTyped(WDEventNames.HISTORY_UPDATED.getName(),      h, mHist); }

    public void onNavigationCommitted(Consumer<WDBrowsingContextEvent.NavigationCommitted> h){ subscribeTyped(WDEventNames.NAVIGATION_COMMITTED.getName(), h, mCommit,   WDBrowsingContextEvent.NavigationCommitted.class); }
    public void offNavigationCommitted(Consumer<WDBrowsingContextEvent.NavigationCommitted> h){ unsubscribeTyped(WDEventNames.NAVIGATION_COMMITTED.getName(),h, mCommit); }

    public void onNavigationAborted(Consumer<WDBrowsingContextEvent.NavigationAborted> h)   { subscribeTyped(WDEventNames.NAVIGATION_ABORTED.getName(),    h, mAbort,    WDBrowsingContextEvent.NavigationAborted.class); }
    public void offNavigationAborted(Consumer<WDBrowsingContextEvent.NavigationAborted> h)  { unsubscribeTyped(WDEventNames.NAVIGATION_ABORTED.getName(),   h, mAbort); }

    public void onNavigationFailed(Consumer<WDBrowsingContextEvent.NavigationFailed> h)     { subscribeTyped(WDEventNames.NAVIGATION_FAILED.getName(),     h, mFail,     WDBrowsingContextEvent.NavigationFailed.class); }
    public void offNavigationFailed(Consumer<WDBrowsingContextEvent.NavigationFailed> h)    { unsubscribeTyped(WDEventNames.NAVIGATION_FAILED.getName(),    h, mFail); }

    // Script / Log
    public void onScriptMessage(Consumer<WDScriptEvent.Message> h) { subscribeTyped(WDEventNames.MESSAGE.getName(),     h, mScript, WDScriptEvent.Message.class); }
    public void offScriptMessage(Consumer<WDScriptEvent.Message> h){ unsubscribeTyped(WDEventNames.MESSAGE.getName(),    h, mScript); }

    public void onLogEntryAdded(Consumer<WDLogEvent.EntryAdded> h) { subscribeTyped(WDEventNames.ENTRY_ADDED.getName(), h, mLog,    WDLogEvent.EntryAdded.class); }
    public void offLogEntryAdded(Consumer<WDLogEvent.EntryAdded> h){ unsubscribeTyped(WDEventNames.ENTRY_ADDED.getName(),h, mLog); }

    /** Entfernt alle registrierten Listener. */
    public void detachAll() {
        // pro Map alles deregistrieren
        mNetBefore.forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.BEFORE_REQUEST_SENT.getName(), page.getBrowsingContextId(), v));
        mNetStart .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.RESPONSE_STARTED.getName(), page.getBrowsingContextId(), v));
        mNetDone  .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.RESPONSE_COMPLETED.getName(), page.getBrowsingContextId(), v));
        mNetErr   .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.FETCH_ERROR.getName(), page.getBrowsingContextId(), v));

        mNavStart.forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.NAVIGATION_STARTED.getName(), page.getBrowsingContextId(), v));
        mFrag    .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.FRAGMENT_NAVIGATED.getName(), page.getBrowsingContextId(), v));
        mHist    .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.HISTORY_UPDATED.getName(), page.getBrowsingContextId(), v));
        mCommit  .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.NAVIGATION_COMMITTED.getName(), page.getBrowsingContextId(), v));
        mAbort   .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.NAVIGATION_ABORTED.getName(), page.getBrowsingContextId(), v));
        mFail    .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.NAVIGATION_FAILED.getName(), page.getBrowsingContextId(), v));

        mScript  .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.MESSAGE.getName(), page.getBrowsingContextId(), v));
        mLog     .forEach((k,v) -> page.getWebDriver().removeEventListener(WDEventNames.ENTRY_ADDED.getName(), page.getBrowsingContextId(), v));

        mNetBefore.clear(); mNetStart.clear(); mNetDone.clear(); mNetErr.clear();
        mNavStart.clear();  mFrag.clear();     mHist.clear();    mCommit.clear(); mAbort.clear(); mFail.clear();
        mScript.clear();    mLog.clear();
    }

    // ---------- intern: generisches Subscribe/Unsubscribe ----------

    private <E> void subscribeTyped(String eventName,
                                    Consumer<E> external,
                                    Map<Consumer<E>, Consumer<Object>> store,
                                    Class<E> eventClass) {
        if (external == null) return;

        Consumer<Object> internal = new Consumer<Object>() {
            @Override public void accept(Object ev) {
                if (eventClass.isInstance(ev)) {
                    external.accept(eventClass.cast(ev));
                } else if (ev instanceof JsonObject) {
                    // Fallback: mappe Json zu gewünschter Klasse
                    try {
                        E mapped = JsonToPlaywrightMapper.mapToInterface((JsonObject) ev, eventClass);
                        if (mapped != null) external.accept(mapped);
                    } catch (Throwable ignored) { /* not our type */ }
                }
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
}
