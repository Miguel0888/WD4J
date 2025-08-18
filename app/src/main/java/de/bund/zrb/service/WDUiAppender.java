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

/** Verkabelt alle WD-Page-Events und schreibt sie als Text ins UI. */
final class WDUiAppender {
    private final Page page;
    private final RecorderTabUi ui;
    private final List<Runnable> detachers = new ArrayList<Runnable>();

    private WDUiAppender(Page page, RecorderTabUi ui) {
        this.page = page;
        this.ui = ui;
    }

    static WDUiAppender attachToPage(Page page, RecorderTabUi ui) {
        WDUiAppender a = new WDUiAppender(page, ui);
        a.registerAllPageEvents();
        return a;
    }

    void detachAll() {
        for (Runnable r : detachers) try { r.run(); } catch (Throwable ignore) {}
        detachers.clear();
    }

    // --- nur Page-Mode (Context analog später) ---
    private void registerAllPageEvents() {
        if (!(page instanceof WDPageExtension)) return;
        WDPageExtension ext = (WDPageExtension) page;

        // helper: jedes on/off-Paar sauber registrieren
        Consumer<WDNetworkEvent.BeforeRequestSent> c1 = e -> ui.appendEvent(e);
        ext.onBeforeRequestSent(c1);
        detachers.add(() -> ext.offBeforeRequestSent(c1));

        Consumer<WDNetworkEvent.ResponseStarted> c2 = e -> ui.appendEvent(e);
        ext.onResponseStarted(c2);
        detachers.add(() -> ext.offResponseStarted(c2));

        Consumer<WDNetworkEvent.ResponseCompleted> c3 = e -> ui.appendEvent(e);
        ext.onResponseCompleted(c3);
        detachers.add(() -> ext.offResponseCompleted(c3));

        Consumer<WDNetworkEvent.FetchError> c4 = e -> ui.appendEvent(e);
        ext.onFetchError(c4);
        detachers.add(() -> ext.offFetchError(c4));

        Consumer<WDBrowsingContextEvent.NavigationStarted> c5 = e -> ui.appendEvent(e);
        ext.onNavigationStarted(c5);
        detachers.add(() -> ext.offNavigationStarted(c5));

        Consumer<WDBrowsingContextEvent.FragmentNavigated> c6 = e -> ui.appendEvent(e);
        ext.onFragmentNavigated(c6);
        detachers.add(() -> ext.offFragmentNavigated(c6));

        Consumer<WDBrowsingContextEvent.HistoryUpdated> c7 = e -> ui.appendEvent(e);
        ext.onHistoryUpdated(c7);
        detachers.add(() -> ext.offHistoryUpdated(c7));

        Consumer<WDBrowsingContextEvent.NavigationCommitted> c8 = e -> ui.appendEvent(e);
        ext.onNavigationCommitted(c8);
        detachers.add(() -> ext.offNavigationCommitted(c8));

        Consumer<WDBrowsingContextEvent.NavigationAborted> c9 = e -> ui.appendEvent(e);
        ext.onNavigationAborted(c9);
        detachers.add(() -> ext.offNavigationAborted(c9));

        Consumer<WDBrowsingContextEvent.NavigationFailed> c10 = e -> ui.appendEvent(e);
        ext.onNavigationFailed(c10);
        detachers.add(() -> ext.offNavigationFailed(c10));

        Consumer<WDScriptEvent.Message> c11 = e -> ui.appendEvent(e);
        ext.onScriptMessage(c11);
        detachers.add(() -> ext.offScriptMessage(c11));

        Consumer<WDLogEvent.EntryAdded> c12 = e -> ui.appendEvent(e);
        ext.onLogEntryAdded(c12);
        detachers.add(() -> ext.offLogEntryAdded(c12));
    }
}
