package de.bund.zrb.service;

import com.microsoft.playwright.*;
import de.bund.zrb.ext.WDPageExtension;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.event.WDScriptEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Verkabelt WD-Extension-Events (nicht gemappt) + alle Playwright-Page-Events und leitet sie an einen Consumer weiter. */
final class WDUiAppender {
    private final Page page;
    private final Consumer<Object> sink;
    private final List<Runnable> detachers = new ArrayList<>();

    private WDUiAppender(Page page, Consumer<Object> sink) {
        this.page = page;
        this.sink = sink != null ? sink : o -> {};
    }

    static WDUiAppender attachToPage(Page page, Consumer<Object> sink) {
        WDUiAppender a = new WDUiAppender(page, sink);
        a.registerPlaywrightEvents();
        a.registerExtensionEvents();
        return a;
    }

    void detachAll() {
        for (Runnable r : detachers) {
            try { r.run(); } catch (Throwable ignore) {}
        }
        detachers.clear();
    }

    // ---------- Playwright-Events (standard API) ----------
    private void registerPlaywrightEvents() {
        // Lifecycle / page-level
        Consumer<Page> onClose = sink::accept;
        page.onClose(onClose);
        detachers.add(() -> page.offClose(onClose));

        Consumer<ConsoleMessage> onConsole = sink::accept;
        page.onConsoleMessage(onConsole);
        detachers.add(() -> page.offConsoleMessage(onConsole));

        // Not supported yet by WebDriver BiDi
//        Consumer<Page> onCrash = sink::accept;
//        page.onCrash(onCrash);
//        detachers.add(() -> page.offCrash(onCrash));

        Consumer<Dialog> onDialog = sink::accept;
        page.onDialog(onDialog);
        detachers.add(() -> page.offDialog(onDialog));

        Consumer<Page> onDom = sink::accept;
        page.onDOMContentLoaded(onDom);
        detachers.add(() -> page.offDOMContentLoaded(onDom));

        Consumer<Page> onLoad = sink::accept;
        page.onLoad(onLoad);
        detachers.add(() -> page.offLoad(onLoad));

        // Network
        Consumer<Request> onReq = sink::accept;
        page.onRequest(onReq);
        detachers.add(() -> page.offRequest(onReq));

        Consumer<Request> onReqFailed = sink::accept;
        page.onRequestFailed(onReqFailed);
        detachers.add(() -> page.offRequestFailed(onReqFailed));

        Consumer<Request> onReqFinished = sink::accept;
        page.onRequestFinished(onReqFinished);
        detachers.add(() -> page.offRequestFinished(onReqFinished));

        Consumer<Response> onResp = sink::accept;
        page.onResponse(onResp);
        detachers.add(() -> page.offResponse(onResp));

        // WebSocket / Worker
        Consumer<WebSocket> onWs = sink::accept;
        page.onWebSocket(onWs);
        detachers.add(() -> page.offWebSocket(onWs));

        Consumer<Worker> onWorker = sink::accept;
        page.onWorker(onWorker);
        detachers.add(() -> page.offWorker(onWorker));

        // Downloads / FileChooser
        Consumer<Download> onDl = sink::accept;
        page.onDownload(onDl);
        detachers.add(() -> page.offDownload(onDl));

        Consumer<FileChooser> onFc = sink::accept;
        page.onFileChooser(onFc);
        detachers.add(() -> page.offFileChooser(onFc));

        // Frames / Popup (sind in deinem Page-Interface vorhanden)
        Consumer<Frame> onFrameAttach = sink::accept;
        page.onFrameAttached(onFrameAttach);
        detachers.add(() -> page.offFrameAttached(onFrameAttach));

        Consumer<Frame> onFrameDetach = sink::accept;
        page.onFrameDetached(onFrameDetach);
        detachers.add(() -> page.offFrameDetached(onFrameDetach));

        Consumer<Frame> onFrameNav = sink::accept;
        page.onFrameNavigated(onFrameNav);
        detachers.add(() -> page.offFrameNavigated(onFrameNav));

        Consumer<Page> onPopup = sink::accept;
        page.onPopup(onPopup);
        detachers.add(() -> page.offPopup(onPopup));
    }

    // ---------- WD-Extension-Events (nur die, die NICHT via Playwright gemappt sind) ----------
    private void registerExtensionEvents() {
        if (!(page instanceof WDPageExtension)) return;
        WDPageExtension ext = (WDPageExtension) page;

        // BrowsingContext: fragmentNavigated, historyUpdated, navigationCommitted, navigationAborted
        Consumer<WDBrowsingContextEvent.FragmentNavigated> cFrag = sink::accept;
        ext.onFragmentNavigated(cFrag);
        detachers.add(() -> ext.offFragmentNavigated(cFrag));

        // Not supported yet by WebDriver BiDi
//        Consumer<WDBrowsingContextEvent.HistoryUpdated> cHist = sink::accept;
//        ext.onHistoryUpdated(cHist);
//        detachers.add(() -> ext.offHistoryUpdated(cHist));

        Consumer<WDBrowsingContextEvent.NavigationCommitted> cCommit = sink::accept;
        ext.onNavigationCommitted(cCommit);
        detachers.add(() -> ext.offNavigationCommitted(cCommit));

        Consumer<WDBrowsingContextEvent.NavigationAborted> cAbort = sink::accept;
        ext.onNavigationAborted(cAbort);
        detachers.add(() -> ext.offNavigationAborted(cAbort));

        // Script: channel message
        Consumer<WDScriptEvent.Message> cMsg = sink::accept;
        ext.onScriptMessage(cMsg);
        detachers.add(() -> ext.offScriptMessage(cMsg));
    }
}
