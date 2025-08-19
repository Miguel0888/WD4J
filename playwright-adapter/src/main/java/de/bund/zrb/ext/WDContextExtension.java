package de.bund.zrb.ext;

import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.event.WDScriptEvent;

import java.util.function.Consumer;

/** Context-API für WD-Events, die NICHT im PlaywrightEventMapper auf PW-Interfaces gemappt werden. */
public interface WDContextExtension {
    /** Muss von UserContextImpl genau eine Instanz liefern. */
    WDContextExtensionSupport wdCtxExt();

    // --- BrowsingContext (nicht gemappt) ---
    default void onFragmentNavigated(Consumer<WDBrowsingContextEvent.FragmentNavigated> h)  { wdCtxExt().onFragmentNavigated(h); }
    default void offFragmentNavigated(Consumer<WDBrowsingContextEvent.FragmentNavigated> h) { wdCtxExt().offFragmentNavigated(h); }

    default void onHistoryUpdated(Consumer<WDBrowsingContextEvent.HistoryUpdated> h)        { wdCtxExt().onHistoryUpdated(h); }
    default void offHistoryUpdated(Consumer<WDBrowsingContextEvent.HistoryUpdated> h)       { wdCtxExt().offHistoryUpdated(h); }

    default void onNavigationCommitted(Consumer<WDBrowsingContextEvent.NavigationCommitted> h)  { wdCtxExt().onNavigationCommitted(h); }
    default void offNavigationCommitted(Consumer<WDBrowsingContextEvent.NavigationCommitted> h) { wdCtxExt().offNavigationCommitted(h); }

    default void onNavigationAborted(Consumer<WDBrowsingContextEvent.NavigationAborted> h)  { wdCtxExt().onNavigationAborted(h); }
    default void offNavigationAborted(Consumer<WDBrowsingContextEvent.NavigationAborted> h) { wdCtxExt().offNavigationAborted(h); }

    default void onUserPromptClosed(Consumer<WDBrowsingContextEvent.UserPromptClosed> h)    { wdCtxExt().onUserPromptClosed(h); }
    default void offUserPromptClosed(Consumer<WDBrowsingContextEvent.UserPromptClosed> h)   { wdCtxExt().offUserPromptClosed(h); }

    // --- Network (nicht gemappt) ---
    default void onAuthRequired(Consumer<WDNetworkEvent.AuthRequired> h) { wdCtxExt().onAuthRequired(h); }
    default void offAuthRequired(Consumer<WDNetworkEvent.AuthRequired> h){ wdCtxExt().offAuthRequired(h); }

    // --- Script (nicht gemappt) ---
    default void onRealmDestroyed(Consumer<WDScriptEvent.RealmDestroyed> h) { wdCtxExt().onRealmDestroyed(h); }
    default void offRealmDestroyed(Consumer<WDScriptEvent.RealmDestroyed> h){ wdCtxExt().offRealmDestroyed(h); }

    // --- Channels / script.message ---
    default void onScriptMessage(Consumer<WDScriptEvent.MessageWD> h) { wdCtxExt().onScriptMessage(h); }
    default void offScriptMessage(Consumer<WDScriptEvent.MessageWD> h){ wdCtxExt().offScriptMessage(h); }

    /** Alles sauber deregistrieren. */
    default void detachAllWdCtx() { wdCtxExt().detachAll(); }
}
