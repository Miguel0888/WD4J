package de.bund.zrb.service;

import com.microsoft.playwright.*;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.ext.WDPageExtension;
import de.bund.zrb.websocket.WDEventNames;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Verkabelt WD-Extension-Events (nicht gemappt) + alle Playwright-Events. */
final class WDUiAppender {
    private final Page page;                  // Page-Mode
    private final BrowserContext ctx;         // Context-Mode
    private final BiConsumer<String, Object> sink; // <BiDi-Eventname, Event>
    private final List<Runnable> detachers = new ArrayList<>();
    private final List<WDUiAppender> childPageAppenders = new ArrayList<>();

    private WDUiAppender(Page page, BiConsumer<String, Object> sink) {
        this.page = page;
        this.ctx = null;
        this.sink = sink != null ? sink : (n, o) -> {};
    }

    private WDUiAppender(BrowserContext ctx, BiConsumer<String, Object> sink) {
        this.page = null;
        this.ctx = ctx;
        this.sink = sink != null ? sink : (n, o) -> {};
    }

    // ---- Page-Mode ----
    static WDUiAppender attachToPage(Page page, BiConsumer<String, Object> sink) {
        WDUiAppender a = new WDUiAppender(page, sink);
        a.registerPlaywrightPageEvents();
        a.registerExtensionPageEvents();
        return a;
    }

    // ---- Context-Mode ----
    static WDUiAppender attachToContext(BrowserContext ctx, BiConsumer<String, Object> sink) {
        WDUiAppender a = new WDUiAppender(ctx, sink);
        a.registerPlaywrightContextEvents();
        registerExtensionContextEvents(ctx, a);

        // existierende + zukünftige Pages
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
        Consumer<Page> onClose = p -> sink.accept(WDEventNames.CONTEXT_DESTROYED.getName(), p);
        page.onClose(onClose);
        detachers.add(() -> page.offClose(onClose));

        Consumer<ConsoleMessage> onConsole = c -> sink.accept(WDEventNames.ENTRY_ADDED.getName(), c);
        page.onConsoleMessage(onConsole);
        detachers.add(() -> page.offConsoleMessage(onConsole));

        Consumer<Dialog> onDialog = d -> sink.accept(WDEventNames.USER_PROMPT_OPENED.getName(), d);
        page.onDialog(onDialog);
        detachers.add(() -> page.offDialog(onDialog));

        Consumer<Page> onDom = p -> sink.accept(WDEventNames.DOM_CONTENT_LOADED.getName(), p);
        page.onDOMContentLoaded(onDom);
        detachers.add(() -> page.offDOMContentLoaded(onDom));

        Consumer<Page> onLoad = p -> sink.accept(WDEventNames.LOAD.getName(), p);
        page.onLoad(onLoad);
        detachers.add(() -> page.offLoad(onLoad));

        // Network
        Consumer<Request> onReq = r -> sink.accept(WDEventNames.BEFORE_REQUEST_SENT.getName(), r);
        page.onRequest(onReq);
        detachers.add(() -> page.offRequest(onReq));

        Consumer<Request> onReqFailed = r -> sink.accept(WDEventNames.FETCH_ERROR.getName(), r);
        page.onRequestFailed(onReqFailed);
        detachers.add(() -> page.offRequestFailed(onReqFailed));

        Consumer<Request> onReqFinished = r -> sink.accept(WDEventNames.RESPONSE_COMPLETED.getName(), r);
        page.onRequestFinished(onReqFinished);
        detachers.add(() -> page.offRequestFinished(onReqFinished));

        Consumer<Response> onResp = r -> sink.accept(WDEventNames.RESPONSE_STARTED.getName(), r);
        page.onResponse(onResp);
        detachers.add(() -> page.offResponse(onResp));

        // WebSocket / Worker
        Consumer<WebSocket> onWs = ws -> sink.accept("playwright.webSocket", ws);
        page.onWebSocket(onWs);
        detachers.add(() -> page.offWebSocket(onWs));

        Consumer<Worker> onWorker = w -> sink.accept(WDEventNames.REALM_CREATED.getName(), w);
        page.onWorker(onWorker);
        detachers.add(() -> page.offWorker(onWorker));

        // Downloads / FileChooser (falls aktiv)
        // Consumer<Download> onDl = d -> sink.accept(WDEventNames.DOWNLOAD_WILL_BEGIN.getName(), d);
        // page.onDownload(onDl);
        // detachers.add(() -> page.offDownload(onDl));
        //
        // Consumer<FileChooser> onFc = fc -> sink.accept(WDEventNames.FILE_DIALOG_OPENED.getName(), fc);
        // page.onFileChooser(onFc);
        // detachers.add(() -> page.offFileChooser(onFc));

        // Frames / Popup
        Consumer<Frame> onFrameAttach = f -> sink.accept(WDEventNames.CONTEXT_CREATED.getName(), f);
        page.onFrameAttached(onFrameAttach);
        detachers.add(() -> page.offFrameAttached(onFrameAttach));

        Consumer<Frame> onFrameDetach = f -> sink.accept(WDEventNames.CONTEXT_DESTROYED.getName(), f);
        page.onFrameDetached(onFrameDetach);
        detachers.add(() -> page.offFrameDetached(onFrameDetach));

        Consumer<Frame> onFrameNav = f -> sink.accept(WDEventNames.NAVIGATION_STARTED.getName(), f);
        page.onFrameNavigated(onFrameNav);
        detachers.add(() -> page.offFrameNavigated(onFrameNav));

        Consumer<Page> onPopup = p -> sink.accept(WDEventNames.CONTEXT_CREATED.getName(), p);
        page.onPopup(onPopup);
        detachers.add(() -> page.offPopup(onPopup));
    }

    // ---------- Playwright: Context ----------
    private void registerPlaywrightContextEvents() {
        if (ctx == null) return;

        Consumer<Request> onReq = r -> sink.accept(WDEventNames.BEFORE_REQUEST_SENT.getName(), r);
        ctx.onRequest(onReq);
        detachers.add(() -> ctx.offRequest(onReq));

        Consumer<Request> onReqFailed = r -> sink.accept(WDEventNames.FETCH_ERROR.getName(), r);
        ctx.onRequestFailed(onReqFailed);
        detachers.add(() -> ctx.offRequestFailed(onReqFailed));

        Consumer<Request> onReqFinished = r -> sink.accept(WDEventNames.RESPONSE_COMPLETED.getName(), r);
        ctx.onRequestFinished(onReqFinished);
        detachers.add(() -> ctx.offRequestFinished(onReqFinished));

        Consumer<Response> onResp = r -> sink.accept(WDEventNames.RESPONSE_STARTED.getName(), r);
        ctx.onResponse(onResp);
        detachers.add(() -> ctx.offResponse(onResp));
    }

    // ---------- WD-Extension: Page ----------
    private void registerExtensionPageEvents() {
        if (!(page instanceof WDPageExtension)) return;
        WDPageExtension ext = (WDPageExtension) page;

        Consumer<de.bund.zrb.event.WDBrowsingContextEvent.FragmentNavigated> cFrag =
                e -> sink.accept(WDEventNames.FRAGMENT_NAVIGATED.getName(), e);
        ext.onFragmentNavigated(cFrag);
        detachers.add(() -> ext.offFragmentNavigated(cFrag));

        // Falls wieder aktiv:
        // Consumer<WDBrowsingContextEvent.NavigationCommitted> cCommit =
        //     e -> sink.accept(WDEventNames.NAVIGATION_COMMITTED.getName(), e);
        // ext.onNavigationCommitted(cCommit);
        // detachers.add(() -> ext.offNavigationCommitted(cCommit));
        //
        // Consumer<WDBrowsingContextEvent.NavigationAborted> cAbort =
        //     e -> sink.accept(WDEventNames.NAVIGATION_ABORTED.getName(), e);
        // ext.onNavigationAborted(cAbort);
        // detachers.add(() -> ext.offNavigationAborted(cAbort));

        Consumer<WDScriptEvent.Message> cMsg =
                e -> sink.accept(WDEventNames.MESSAGE.getName(), e);
        ext.onScriptMessage(cMsg);
        detachers.add(() -> ext.offScriptMessage(cMsg));
    }

    // ---------- WD-Extension: Context ----------
    private static void registerExtensionContextEvents(BrowserContext ctx, WDUiAppender a) {
        if (!(ctx instanceof de.bund.zrb.ext.WDContextExtension)) return;
        de.bund.zrb.ext.WDContextExtension ext = (de.bund.zrb.ext.WDContextExtension) ctx;

        Consumer<de.bund.zrb.event.WDBrowsingContextEvent.FragmentNavigated> cFrag =
                e -> a.sink.accept(WDEventNames.FRAGMENT_NAVIGATED.getName(), e);
        ext.onFragmentNavigated(cFrag);
        a.detachers.add(() -> ext.offFragmentNavigated(cFrag));

        // Falls wieder aktiv:
        // Consumer<WDBrowsingContextEvent.NavigationCommitted> cCommit =
        //     e -> a.sink.accept(WDEventNames.NAVIGATION_COMMITTED.getName(), e);
        // ext.onNavigationCommitted(cCommit);
        // a.detachers.add(() -> ext.offNavigationCommitted(cCommit));
        //
        // Consumer<WDBrowsingContextEvent.NavigationAborted> cAbort =
        //     e -> a.sink.accept(WDEventNames.NAVIGATION_ABORTED.getName(), e);
        // ext.onNavigationAborted(cAbort);
        // a.detachers.add(() -> ext.offNavigationAborted(cAbort));

        Consumer<de.bund.zrb.event.WDScriptEvent.Message> cMsg =
                e -> a.sink.accept(WDEventNames.MESSAGE.getName(), e);
        ext.onScriptMessage(cMsg);
        a.detachers.add(() -> ext.offScriptMessage(cMsg));
    }
}
