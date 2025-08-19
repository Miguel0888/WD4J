package de.bund.zrb.service;

import com.microsoft.playwright.*;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.ext.WDPageExtension;

import java.util.*;
import java.util.function.Consumer;

/** Verkabelt WD-Extension-Events (nicht gemappt) + alle Playwright-Events. */
final class WDUiAppender {
    private final Page page; // bleibt für Page-Mode
    private final BrowserContext ctx; // neu: für Context-Mode
    private final Consumer<Object> sink;
    private final List<Runnable> detachers = new ArrayList<>();
    private final List<WDUiAppender> childPageAppenders = new ArrayList<>(); // für Context-Mode (pro Page ein Appender)

    // ---- Konstruktoren ----
    private WDUiAppender(Page page, Consumer<Object> sink) {
        this.page = page;
        this.ctx = null;
        this.sink = sink != null ? sink : o -> {};
    }

    private WDUiAppender(BrowserContext ctx, Consumer<Object> sink) {
        this.page = null;
        this.ctx = ctx;
        this.sink = sink != null ? sink : o -> {};
    }

    // ---- Page-Mode (bestehend) ----
    static WDUiAppender attachToPage(Page page, Consumer<Object> sink) {
        WDUiAppender a = new WDUiAppender(page, sink);
        a.registerPlaywrightPageEvents();
        a.registerExtensionPageEvents();
        return a;
    }

    // ---- Context-Mode (neu) ----
    static WDUiAppender attachToContext(BrowserContext ctx, Consumer<Object> sink) {
        WDUiAppender a = new WDUiAppender(ctx, sink);
        a.registerPlaywrightContextEvents();
        registerExtensionContextEvents(ctx, a);

        // existierende Pages + zukünftige Pages → schon vorhanden in deiner attachToContext-Logik
        for (Page p : ctx.pages()) {
            if (p instanceof WDPageExtension) {
                WDUiAppender child = attachToPage(p, sink);
                a.childPageAppenders.add(child);
            }
        }
        Consumer<Page> onPage = p -> {
            if (p instanceof WDPageExtension) {
                WDUiAppender child = attachToPage(p, sink);
                a.childPageAppenders.add(child);
            }
        };
        ctx.onPage(onPage);
        a.detachers.add(() -> ctx.offPage(onPage));

        return a;
    }

    void detachAll() {
        // erst Kinder (Page-Appender) lösen
        for (WDUiAppender c : childPageAppenders) {
            try { c.detachAll(); } catch (Throwable ignore) {}
        }
        childPageAppenders.clear();

        for (Runnable r : detachers) {
            try { r.run(); } catch (Throwable ignore) {}
        }
        detachers.clear();
    }

    // ---------- Playwright: Page ----------
    private void registerPlaywrightPageEvents() {
        if (page == null) return;

        // Lifecycle / page-level
        Consumer<Page> onClose = sink::accept;
        page.onClose(onClose);
        detachers.add(() -> page.offClose(onClose));

        Consumer<ConsoleMessage> onConsole = sink::accept;
        page.onConsoleMessage(onConsole);
        detachers.add(() -> page.offConsoleMessage(onConsole));

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
        // Currently not supported yet by WebDriverBiDi
//        Consumer<Download> onDl = sink::accept;
//        page.onDownload(onDl);
//        detachers.add(() -> page.offDownload(onDl));

//        Consumer<FileChooser> onFc = sink::accept;
//        page.onFileChooser(onFc);
//        detachers.add(() -> page.offFileChooser(onFc));

        // Frames / Popup
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

    // ---------- Playwright: Context ----------
    private void registerPlaywrightContextEvents() {
        if (ctx == null) return;

        // Nur die von deinem UserContextImpl unterstützten Events
        Consumer<Request> onReq = sink::accept;
        ctx.onRequest(onReq);
        detachers.add(() -> ctx.offRequest(onReq));

        Consumer<Request> onReqFailed = sink::accept;
        ctx.onRequestFailed(onReqFailed);
        detachers.add(() -> ctx.offRequestFailed(onReqFailed));

        Consumer<Request> onReqFinished = sink::accept;
        ctx.onRequestFinished(onReqFinished);
        detachers.add(() -> ctx.offRequestFinished(onReqFinished));

        Consumer<Response> onResp = sink::accept;
        ctx.onResponse(onResp);
        detachers.add(() -> ctx.offResponse(onResp));

        // Neue Pages spiegelt attachToContext() separat (siehe onPage oben)
    }

    // ---------- WD-Extension: Page ----------
    private void registerExtensionPageEvents() {
        if (!(page instanceof WDPageExtension)) return;
        WDPageExtension ext = (WDPageExtension) page;

        Consumer<de.bund.zrb.event.WDBrowsingContextEvent.FragmentNavigated> cFrag = sink::accept;
        ext.onFragmentNavigated(cFrag);
        detachers.add(() -> ext.offFragmentNavigated(cFrag));

        // Currently not supported yet by WebDriverBiDi
//        Consumer<de.bund.zrb.event.WDBrowsingContextEvent.NavigationCommitted> cCommit = sink::accept;
//        ext.onNavigationCommitted(cCommit);
//        detachers.add(() -> ext.offNavigationCommitted(cCommit));

        // Currently not supported yet by WebDriverBiDi
//        Consumer<de.bund.zrb.event.WDBrowsingContextEvent.NavigationAborted> cAbort = sink::accept;
//        ext.onNavigationAborted(cAbort);
//        detachers.add(() -> ext.offNavigationAborted(cAbort));

        // Script: channel message
        Consumer<WDScriptEvent.Message> cMsg = sink::accept;
        ext.onScriptMessage(cMsg);
        detachers.add(() -> ext.offScriptMessage(cMsg));

        // HistoryUpdated hast du aktuell deaktiviert → weggelassen
        // AuthRequired/RealmDestroyed liegen auf der Extension, falls du sie wieder aktivierst, kannst du sie hier analog eintragen.
    }

    private static void registerExtensionContextEvents(BrowserContext ctx, WDUiAppender a) {
        if (!(ctx instanceof de.bund.zrb.ext.WDContextExtension)) return;
        de.bund.zrb.ext.WDContextExtension ext = (de.bund.zrb.ext.WDContextExtension) ctx;

        // Nur nicht-gemappte Events
        java.util.function.Consumer<de.bund.zrb.event.WDBrowsingContextEvent.FragmentNavigated> cFrag = a.sink::accept;
        ext.onFragmentNavigated(cFrag);
        a.detachers.add(() -> ext.offFragmentNavigated(cFrag));

        java.util.function.Consumer<de.bund.zrb.event.WDBrowsingContextEvent.NavigationCommitted> cCommit = a.sink::accept;
        ext.onNavigationCommitted(cCommit);
        a.detachers.add(() -> ext.offNavigationCommitted(cCommit));

        java.util.function.Consumer<de.bund.zrb.event.WDBrowsingContextEvent.NavigationAborted> cAbort = a.sink::accept;
        ext.onNavigationAborted(cAbort);
        a.detachers.add(() -> ext.offNavigationAborted(cAbort));

        // Falls gewünscht/aktiv: HistoryUpdated, UserPromptClosed
        // java.util.function.Consumer<de.bund.zrb.event.WDBrowsingContextEvent.HistoryUpdated> cHist = a.sink::accept;
        // ext.onHistoryUpdated(cHist);
        // a.detachers.add(() -> ext.offHistoryUpdated(cHist));

        // Channels / script.message
        java.util.function.Consumer<de.bund.zrb.event.WDScriptEvent.Message> cMsg = a.sink::accept;
        ext.onScriptMessage(cMsg);
        a.detachers.add(() -> ext.offScriptMessage(cMsg));
    }

}
