package de.bund.zrb.ext;

import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDLogEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.event.WDScriptEvent;

import java.util.function.Consumer;

/** Default-API für getypte WD-Events auf Page-Ebene. */
public interface WDPageExtension {
    /** Muss von PageImpl eine einzige Instanz zurückgeben. */
    WDPageExtensionSupport wdExt();

    // --- Network ---
    default void onBeforeRequestSent(Consumer<WDNetworkEvent.BeforeRequestSent> h) { wdExt().onBeforeRequestSent(h); }
    default void offBeforeRequestSent(Consumer<WDNetworkEvent.BeforeRequestSent> h){ wdExt().offBeforeRequestSent(h); }

    default void onResponseStarted(Consumer<WDNetworkEvent.ResponseStarted> h)     { wdExt().onResponseStarted(h); }
    default void offResponseStarted(Consumer<WDNetworkEvent.ResponseStarted> h)    { wdExt().offResponseStarted(h); }

    default void onResponseCompleted(Consumer<WDNetworkEvent.ResponseCompleted> h) { wdExt().onResponseCompleted(h); }
    default void offResponseCompleted(Consumer<WDNetworkEvent.ResponseCompleted> h){ wdExt().offResponseCompleted(h); }

    default void onFetchError(Consumer<WDNetworkEvent.FetchError> h)               { wdExt().onFetchError(h); }
    default void offFetchError(Consumer<WDNetworkEvent.FetchError> h)              { wdExt().offFetchError(h); }

    // --- BrowsingContext ---
    default void onNavigationStarted(Consumer<WDBrowsingContextEvent.NavigationStarted> h)  { wdExt().onNavigationStarted(h); }
    default void offNavigationStarted(Consumer<WDBrowsingContextEvent.NavigationStarted> h) { wdExt().offNavigationStarted(h); }

    default void onFragmentNavigated(Consumer<WDBrowsingContextEvent.FragmentNavigated> h)  { wdExt().onFragmentNavigated(h); }
    default void offFragmentNavigated(Consumer<WDBrowsingContextEvent.FragmentNavigated> h) { wdExt().offFragmentNavigated(h); }

    default void onHistoryUpdated(Consumer<WDBrowsingContextEvent.HistoryUpdated> h)        { wdExt().onHistoryUpdated(h); }
    default void offHistoryUpdated(Consumer<WDBrowsingContextEvent.HistoryUpdated> h)       { wdExt().offHistoryUpdated(h); }

    default void onNavigationCommitted(Consumer<WDBrowsingContextEvent.NavigationCommitted> h)  { wdExt().onNavigationCommitted(h); }
    default void offNavigationCommitted(Consumer<WDBrowsingContextEvent.NavigationCommitted> h) { wdExt().offNavigationCommitted(h); }

    default void onNavigationAborted(Consumer<WDBrowsingContextEvent.NavigationAborted> h)  { wdExt().onNavigationAborted(h); }
    default void offNavigationAborted(Consumer<WDBrowsingContextEvent.NavigationAborted> h) { wdExt().offNavigationAborted(h); }

    default void onNavigationFailed(Consumer<WDBrowsingContextEvent.NavigationFailed> h)    { wdExt().onNavigationFailed(h); }
    default void offNavigationFailed(Consumer<WDBrowsingContextEvent.NavigationFailed> h)   { wdExt().offNavigationFailed(h); }

    // --- Script / Log ---
    default void onScriptMessage(Consumer<WDScriptEvent.Message> h) { wdExt().onScriptMessage(h); }
    default void offScriptMessage(Consumer<WDScriptEvent.Message> h){ wdExt().offScriptMessage(h); }

    default void onLogEntryAdded(Consumer<WDLogEvent.EntryAdded> h) { wdExt().onLogEntryAdded(h); }
    default void offLogEntryAdded(Consumer<WDLogEvent.EntryAdded> h){ wdExt().offLogEntryAdded(h); }

    /** Alle Listener sauber deregistrieren. */
    default void detachAllWd() { wdExt().detachAll(); }
}
