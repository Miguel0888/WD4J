package de.bund.zrb.ext;

import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.event.WDScriptEvent;

import java.util.function.Consumer;

/** Default-API für getypte WD-Events auf Page-Ebene.
 *  Bietet NUR Events an, die im PlaywrightEventMapper NICHT auf ein Playwright-Interface gemappt werden. */
public interface WDPageExtension {
    /** Muss von PageImpl eine einzige Instanz zurückgeben. */
    WDPageExtensionSupport wdExt();

    // --- BrowsingContext (nicht gemappt) ---
    default void onFragmentNavigated(Consumer<WDBrowsingContextEvent.FragmentNavigated> h)  { wdExt().onFragmentNavigated(h); }
    default void offFragmentNavigated(Consumer<WDBrowsingContextEvent.FragmentNavigated> h) { wdExt().offFragmentNavigated(h); }

    default void onHistoryUpdated(Consumer<WDBrowsingContextEvent.HistoryUpdated> h)        { wdExt().onHistoryUpdated(h); }
    default void offHistoryUpdated(Consumer<WDBrowsingContextEvent.HistoryUpdated> h)       { wdExt().offHistoryUpdated(h); }

    default void onNavigationCommitted(Consumer<WDBrowsingContextEvent.NavigationCommitted> h)  { wdExt().onNavigationCommitted(h); }
    default void offNavigationCommitted(Consumer<WDBrowsingContextEvent.NavigationCommitted> h) { wdExt().offNavigationCommitted(h); }

    default void onNavigationAborted(Consumer<WDBrowsingContextEvent.NavigationAborted> h)  { wdExt().onNavigationAborted(h); }
    default void offNavigationAborted(Consumer<WDBrowsingContextEvent.NavigationAborted> h) { wdExt().offNavigationAborted(h); }

    default void onUserPromptClosed(Consumer<WDBrowsingContextEvent.UserPromptClosed> h)    { wdExt().onUserPromptClosed(h); }
    default void offUserPromptClosed(Consumer<WDBrowsingContextEvent.UserPromptClosed> h)   { wdExt().offUserPromptClosed(h); }

    // --- Network (nicht gemappt) ---
    default void onAuthRequired(Consumer<WDNetworkEvent.AuthRequired> h) { wdExt().onAuthRequired(h); }
    default void offAuthRequired(Consumer<WDNetworkEvent.AuthRequired> h){ wdExt().offAuthRequired(h); }

    // --- Script (nicht gemappt) ---
    default void onRealmDestroyed(Consumer<WDScriptEvent.RealmDestroyed> h) { wdExt().onRealmDestroyed(h); }
    default void offRealmDestroyed(Consumer<WDScriptEvent.RealmDestroyed> h){ wdExt().offRealmDestroyed(h); }

    // --- Channels / script.message (kein offizielles PW-Interface) ---
    default void onScriptMessage(Consumer<WDScriptEvent.MessageWD> h) { wdExt().onScriptMessage(h); }
    default void offScriptMessage(Consumer<WDScriptEvent.MessageWD> h){ wdExt().offScriptMessage(h); }

    /** Alle Listener sauber deregistrieren. */
    default void detachAllWd() { wdExt().detachAll(); }
}
