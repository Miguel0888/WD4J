package de.bund.zrb.service;

import com.microsoft.playwright.Page;
import de.bund.zrb.ext.WDPageExtension;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDLogEvent;
import de.bund.zrb.event.WDNetworkEvent;
import de.bund.zrb.event.WDScriptEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Verkabelt alle WD-Page-Events und leitet sie an einen Consumer weiter. */
final class WDUiAppender {
    private final Page page;
    private final Consumer<Object> sink;
    private final List<Runnable> detachers = new ArrayList<Runnable>();

    private WDUiAppender(Page page, Consumer<Object> sink) {
        this.page = page;
        this.sink = sink != null ? sink : o -> {};
    }

    static WDUiAppender attachToPage(Page page, Consumer<Object> sink) {
        WDUiAppender a = new WDUiAppender(page, sink);
        a.registerAllPageEvents();
        return a;
    }

    void detachAll() {
        for (Runnable r : detachers) {
            try { r.run(); } catch (Throwable ignore) {}
        }
        detachers.clear();
    }

    // --- nur Page-Mode (Context analog später) ---
    private void registerAllPageEvents() {
        if (!(page instanceof WDPageExtension)) return;
        WDPageExtension ext = (WDPageExtension) page;

        // helper: jedes on/off-Paar sauber registrieren
        Consumer<WDNetworkEvent.BeforeRequestSent> c1 = sink::accept;
        ext.onBeforeRequestSent(c1);
        detachers.add(() -> ext.offBeforeRequestSent(c1));

        Consumer<WDNetworkEvent.ResponseStarted> c2 = sink::accept;
        ext.onResponseStarted(c2);
        detachers.add(() -> ext.offResponseStarted(c2));

        Consumer<WDNetworkEvent.ResponseCompleted> c3 = sink::accept;
        ext.onResponseCompleted(c3);
        detachers.add(() -> ext.offResponseCompleted(c3));

        Consumer<WDNetworkEvent.FetchError> c4 = sink::accept;
        ext.onFetchError(c4);
        detachers.add(() -> ext.offFetchError(c4));

        Consumer<WDBrowsingContextEvent.NavigationStarted> c5 = sink::accept;
        ext.onNavigationStarted(c5);
        detachers.add(() -> ext.offNavigationStarted(c5));

        Consumer<WDBrowsingContextEvent.FragmentNavigated> c6 = sink::accept;
        ext.onFragmentNavigated(c6);
        detachers.add(() -> ext.offFragmentNavigated(c6));

        Consumer<WDBrowsingContextEvent.HistoryUpdated> c7 = sink::accept;
        ext.onHistoryUpdated(c7);
        detachers.add(() -> ext.offHistoryUpdated(c7));

        Consumer<WDBrowsingContextEvent.NavigationCommitted> c8 = sink::accept;
        ext.onNavigationCommitted(c8);
        detachers.add(() -> ext.offNavigationCommitted(c8));

        Consumer<WDBrowsingContextEvent.NavigationAborted> c9 = sink::accept;
        ext.onNavigationAborted(c9);
        detachers.add(() -> ext.offNavigationAborted(c9));

        Consumer<WDBrowsingContextEvent.NavigationFailed> c10 = sink::accept;
        ext.onNavigationFailed(c10);
        detachers.add(() -> ext.offNavigationFailed(c10));

        Consumer<WDScriptEvent.Message> c11 = sink::accept;
        ext.onScriptMessage(c11);
        detachers.add(() -> ext.offScriptMessage(c11));

        Consumer<WDLogEvent.EntryAdded> c12 = sink::accept;
        ext.onLogEntryAdded(c12);
        detachers.add(() -> ext.offLogEntryAdded(c12));
    }
}
