package wd4j.impl.playwright;

import com.google.gson.JsonObject;
import wd4j.impl.service.BrowsingContextService;
import wd4j.api.*;
import wd4j.api.options.*;
import wd4j.impl.service.ScriptService;
import wd4j.impl.service.SessionService;
import wd4j.impl.webdriver.event.MethodEvent;
import wd4j.impl.support.JsonToPlaywrightMapper;

import java.nio.file.Path;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

class PageImpl implements Page {
    private final BrowserSessionImpl context;
    private final SessionService sessionService;
    private final BrowsingContextService browsingContextService;
    private final ScriptService scriptService;
    private boolean isClosed;
    private String url;
    private WebSocketImpl webSocketImpl;

    public PageImpl(BrowserSessionImpl context) {
        this.context = context;
        this.sessionService = context.getBrowser().getSessionService(); // ToDo: improve this!
        this.browsingContextService = context.getBrowser().getBrowsingContextService(); // ToDo: improve this!
        this.scriptService = context.getBrowser().getScriptService(); // ToDo: improve this!
        this.isClosed = false;
        this.url = "about:blank"; // Standard-Startseite

        webSocketImpl = ((BrowserImpl) context.getBrowser()).getWebSocketConnection();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Standard Features, directly supported by WebDriver
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onClose(Consumer<Page> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.CONTEXT_DESTROYED.getName(), handler, Page.class, sessionService);
        }
    }

    @Override
    public void offClose(Consumer<Page> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.CONTEXT_DESTROYED.getName(), handler, sessionService);
        }
    }

    @Override
    public void onConsoleMessage(Consumer<ConsoleMessage> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.ENTRY_ADDED.getName(), handler, ConsoleMessage.class, sessionService);
        }
    }

    @Override
    public void offConsoleMessage(Consumer<ConsoleMessage> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.ENTRY_ADDED.getName(), handler, sessionService);
        }
    }

    @Override
    public void onCrash(Consumer<Page> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.NAVIGATION_FAILED.getName(), handler, Page.class, sessionService);
        }
    }

    @Override
    public void offCrash(Consumer<Page> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.NAVIGATION_FAILED.getName(), handler, sessionService);
        }
    }

    @Override
    public void onDialog(Consumer<Dialog> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.USER_PROMPT_OPENED.getName(), handler, Dialog.class, sessionService);
        }
    }

    @Override
    public void offDialog(Consumer<Dialog> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.USER_PROMPT_OPENED.getName(), handler, sessionService);
        }
    }

    @Override
    public void onDOMContentLoaded(Consumer<Page> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.DOM_CONTENT_LOADED.getName(), handler, Page.class, sessionService);
        }
    }

    @Override
    public void offDOMContentLoaded(Consumer<Page> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.DOM_CONTENT_LOADED.getName(), handler, sessionService);
        }
    }

    @Override
    public void onLoad(Consumer<Page> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.LOAD.getName(), handler, Page.class, sessionService);
        }
    }

    @Override
    public void offLoad(Consumer<Page> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.LOAD.getName(), handler, sessionService);
        }
    }

    @Override
    public void onRequest(Consumer<Request> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.BEFORE_REQUEST_SENT.getName(), handler, Request.class, sessionService);
        }
    }

    @Override
    public void offRequest(Consumer<Request> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.BEFORE_REQUEST_SENT.getName(), handler, sessionService);
        }
    }

    @Override
    public void onRequestFailed(Consumer<Request> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.FETCH_ERROR.getName(), handler, Request.class, sessionService);
        }
    }

    @Override
    public void offRequestFailed(Consumer<Request> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.FETCH_ERROR.getName(), handler, sessionService);
        }
    }

    @Override
    public void onRequestFinished(Consumer<Request> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.RESPONSE_COMPLETED.getName(), handler, Request.class, sessionService);
        }
    }

    @Override
    public void offRequestFinished(Consumer<Request> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.RESPONSE_COMPLETED.getName(), handler, sessionService);
        }
    }

    @Override
    public void onResponse(Consumer<Response> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.RESPONSE_STARTED.getName(), handler, Response.class, sessionService);
        }
    }

    @Override
    public void offResponse(Consumer<Response> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.RESPONSE_STARTED.getName(), handler, sessionService);
        }
    }

    @Override
    public void onWebSocket(Consumer<WebSocket> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.CONTEXT_CREATED.getName(), handler, WebSocket.class, sessionService);
        }
    }

    @Override
    public void offWebSocket(Consumer<WebSocket> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.CONTEXT_CREATED.getName(), handler, sessionService);
        }
    }

    @Override
    public void onWorker(Consumer<Worker> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.REALM_CREATED.getName(), handler, Worker.class, sessionService);
        }
    }

    @Override
    public void offWorker(Consumer<Worker> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.REALM_CREATED.getName(), handler, sessionService);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Advanced Features, not directly supported by WebDriver
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onDownload(Consumer<Download> handler) {

    }

    @Override
    public void offDownload(Consumer<Download> handler) {

    }

    @Override
    public void onFileChooser(Consumer<FileChooser> handler) {

    }

    @Override
    public void offFileChooser(Consumer<FileChooser> handler) {

    }

    @Override
    public void onFrameAttached(Consumer<Frame> handler) {

    }

    @Override
    public void offFrameAttached(Consumer<Frame> handler) {

    }

    @Override
    public void onFrameDetached(Consumer<Frame> handler) {

    }

    @Override
    public void offFrameDetached(Consumer<Frame> handler) {

    }

    @Override
    public void onFrameNavigated(Consumer<Frame> handler) {

    }

    @Override
    public void offFrameNavigated(Consumer<Frame> handler) {

    }

    @Override
    public void onPageError(Consumer<String> handler) {
        // ToDo: Maybe use "network.fetchError" event?
        //  Or: "log.entryAdded"?
    }

    @Override
    public void offPageError(Consumer<String> handler) {

    }

    @Override
    public void onPopup(Consumer<Page> handler) {
        if (handler != null) {
            webSocketImpl.addEventListener(MethodEvent.CONTEXT_CREATED.getName(), jsonObject -> {
                // Stelle sicher, dass jsonObject tatsächlich ein JsonObject ist
                Page popupPage = JsonToPlaywrightMapper.mapToInterface((JsonObject) jsonObject, Page.class);
                handler.accept(popupPage);
            }, JsonObject.class, sessionService);
        }
    }

    @Override
    public void offPopup(Consumer<Page> handler) {
        if (handler != null) {
            webSocketImpl.removeEventListener(MethodEvent.CONTEXT_CREATED.getName(), handler, sessionService);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Clock clock() {
        return null;
    }

    @Override
    public void addInitScript(String script) {

    }

    @Override
    public void addInitScript(Path script) {

    }

    @Override
    public ElementHandle addScriptTag(AddScriptTagOptions options) {
        return null;
    }

    @Override
    public ElementHandle addStyleTag(AddStyleTagOptions options) {
        return null;
    }

    @Override
    public void bringToFront() {

    }

    @Override
    public void check(String selector, CheckOptions options) {

    }

    @Override
    public void click(String selector, ClickOptions options) {

    }

    @Override
    public void close() {
        if (!isClosed) {
            isClosed = true;
            browsingContextService.close(context.getContextId());
        }
    }

    @Override
    public void close(CloseOptions options) {

    }

    @Override
    public String content() {
        if (isClosed) {
            throw new PlaywrightException("Page is closed");
        }
        return scriptService.evaluate("return document.documentElement.outerHTML;", context.getContextId());
    }


    @Override
    public BrowserContext context() {
        return null;
    }

    @Override
    public void dblclick(String selector, DblclickOptions options) {

    }

    @Override
    public void dispatchEvent(String selector, String type, Object eventInit, DispatchEventOptions options) {

    }

    @Override
    public void dragAndDrop(String source, String target, DragAndDropOptions options) {

    }

    @Override
    public void emulateMedia(EmulateMediaOptions options) {

    }

    @Override
    public Object evalOnSelector(String selector, String expression, Object arg, EvalOnSelectorOptions options) {
        return null;
    }

    @Override
    public Object evalOnSelectorAll(String selector, String expression, Object arg) {
        return null;
    }

    @Override
    public Object evaluate(String expression, Object arg) {
        return null;
    }

    @Override
    public JSHandle evaluateHandle(String expression, Object arg) {
        return null;
    }

    @Override
    public void exposeBinding(String name, BindingCallback callback, ExposeBindingOptions options) {

    }

    @Override
    public void exposeFunction(String name, FunctionCallback callback) {

    }

    @Override
    public void fill(String selector, String value, FillOptions options) {

    }

    @Override
    public void focus(String selector, FocusOptions options) {

    }

    @Override
    public Frame frame(String name) {
        return null;
    }

    @Override
    public Frame frameByUrl(String url) {
        return null;
    }

    @Override
    public Frame frameByUrl(Pattern url) {
        return null;
    }

    @Override
    public Frame frameByUrl(Predicate<String> url) {
        return null;
    }

    @Override
    public FrameLocator frameLocator(String selector) {
        return null;
    }

    @Override
    public List<Frame> frames() {
        return Collections.emptyList();
    }

    @Override
    public String getAttribute(String selector, String name, GetAttributeOptions options) {
        return "";
    }

    @Override
    public Locator getByAltText(String text, GetByAltTextOptions options) {
        return null;
    }

    @Override
    public Locator getByAltText(Pattern text, GetByAltTextOptions options) {
        return null;
    }

    @Override
    public Locator getByLabel(String text, GetByLabelOptions options) {
//        // Nutze WebDriver BiDi "script.evaluate" um das Element mit aria-label oder label zu finden
//        String script = """
//            (parent, labelText) => {
//                let elements = parent.querySelectorAll("input, textarea, select");
//                return Array.from(elements).find(el =>
//                    el.getAttribute("aria-label") === labelText ||
//                    document.querySelector(`label[for="${el.id}"]`)?.textContent === labelText
//                );
//            }
//        """;
//
//        CompletableFuture<WebSocketFrame> futureResponse = webSocketImpl.send(
//                new Script.Evaluate(context.getContextId(), script, List.of(this.selector, labelText))
//        );
//
//        // Den neuen Locator basierend auf dem gefundenen Element zurückgeben
//        return futureResponse.thenApply(frame -> {
//            JsonObject jsonResponse = new Gson().fromJson(frame.text(), JsonObject.class);
//            JsonElement elementId = jsonResponse.get("result");
//            return new LocatorImpl(elementId.getAsString(), context.getContextId(), webSocketImpl);
//        }).join();
        return null;
    }

    @Override
    public Locator getByLabel(Pattern text, GetByLabelOptions options) {
        return null;
    }

    @Override
    public Locator getByPlaceholder(String text, GetByPlaceholderOptions options) {
        return null;
    }

    @Override
    public Locator getByPlaceholder(Pattern text, GetByPlaceholderOptions options) {
        return null;
    }

    @Override
    public Locator getByRole(AriaRole role, GetByRoleOptions options) {
        return null;
    }

    @Override
    public Locator getByTestId(String testId) {
        return null;
    }

    @Override
    public Locator getByTestId(Pattern testId) {
        return null;
    }

    @Override
    public Locator getByText(String text, GetByTextOptions options) {
        return null;
    }

    @Override
    public Locator getByText(Pattern text, GetByTextOptions options) {
        return null;
    }

    @Override
    public Locator getByTitle(String text, GetByTitleOptions options) {
        return null;
    }

    @Override
    public Locator getByTitle(Pattern text, GetByTitleOptions options) {
        return null;
    }

    @Override
    public Response goBack(GoBackOptions options) {
        browsingContextService.traverseHistory(context.getContextId(), -1);

        return null; // ToDo: Echte Response zurückgeben
    }

    @Override
    public Response goForward(GoForwardOptions options) {
        browsingContextService.traverseHistory(context.getContextId(), 1);

        return null; // ToDo: Echte Response zurückgeben
    }

    @Override
    public void requestGC() {

    }

    @Override
    public Response navigate(String url, NavigateOptions options) {
        if (isClosed) {
            throw new PlaywrightException("Page is closed");
        }
        String contextId = context.getContextId();
        if (contextId == null || contextId.isEmpty()) {
            throw new PlaywrightException("Cannot navigate: contextId is null or empty!");
        }
        this.url = url;

        // WebDriver BiDi Befehl senden
        browsingContextService.navigate(url, contextId);

        return null; // ToDo: Echte Response zurückgeben
    }

    @Override
    public void hover(String selector, HoverOptions options) {

    }

    @Override
    public String innerHTML(String selector, InnerHTMLOptions options) {
        return "";
    }

    @Override
    public String innerText(String selector, InnerTextOptions options) {
        return "";
    }

    @Override
    public String inputValue(String selector, InputValueOptions options) {
        return "";
    }

    @Override
    public boolean isChecked(String selector, IsCheckedOptions options) {
        return false;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean isDisabled(String selector, IsDisabledOptions options) {
        return false;
    }

    @Override
    public boolean isEditable(String selector, IsEditableOptions options) {
        return false;
    }

    @Override
    public boolean isEnabled(String selector, IsEnabledOptions options) {
        return false;
    }

    @Override
    public boolean isHidden(String selector, IsHiddenOptions options) {
        return false;
    }

    @Override
    public boolean isVisible(String selector, IsVisibleOptions options) {
        return false;
    }

    @Override
    public Keyboard keyboard() {
        return null;
    }

    @Override
    public Locator locator(String selector, LocatorOptions options) {
        if (selector == null || selector.isEmpty()) {
            throw new IllegalArgumentException("Selector must not be null or empty.");
        }
        return new LocatorImpl(selector, context.getContextId(), webSocketImpl);

        // ToDo: Implementierung verbessern
        // XPath-Selektoren beginnen mit "xpath=", CSS-Selektoren mit "css="
//        if (selector.startsWith("xpath=")) {
//            return new LocatorImpl(selector);
//        }
//        return new LocatorImpl("css=" + selector);
    }

    @Override
    public Frame mainFrame() {
        return null;
    }

    @Override
    public Mouse mouse() {
        return null;
    }

    @Override
    public void onceDialog(Consumer<Dialog> handler) {

    }

    @Override
    public Page opener() {
        return null;
    }

    @Override
    public void pause() {

    }

    @Override
    public byte[] pdf(PdfOptions options) {
        return new byte[0];
    }

    @Override
    public void press(String selector, String key, PressOptions options) {

    }

    @Override
    public ElementHandle querySelector(String selector, QuerySelectorOptions options) {
        return null;
    }

    @Override
    public List<ElementHandle> querySelectorAll(String selector) {
        return Collections.emptyList();
    }

    @Override
    public void addLocatorHandler(Locator locator, Consumer<Locator> handler, AddLocatorHandlerOptions options) {

    }

    @Override
    public void removeLocatorHandler(Locator locator) {

    }

    @Override
    public Response reload(ReloadOptions options) {
        browsingContextService.reload(context.getContextId());

        return null; // ToDo: Echte Response zurückgeben
    }

    @Override
    public APIRequestContext request() {
        return null;
    }

    @Override
    public void route(String url, Consumer<Route> handler, RouteOptions options) {

    }

    @Override
    public void route(Pattern url, Consumer<Route> handler, RouteOptions options) {

    }

    @Override
    public void route(Predicate<String> url, Consumer<Route> handler, RouteOptions options) {

    }

    @Override
    public void routeFromHAR(Path har, RouteFromHAROptions options) {

    }

    @Override
    public void routeWebSocket(String url, Consumer<WebSocketRoute> handler) {

    }

    @Override
    public void routeWebSocket(Pattern url, Consumer<WebSocketRoute> handler) {

    }

    @Override
    public void routeWebSocket(Predicate<String> url, Consumer<WebSocketRoute> handler) {

    }

    @Override
    public byte[] screenshot(ScreenshotOptions options) {
        String base64Image = browsingContextService.captureScreenshot(context.getContextId());
        return Base64.getDecoder().decode(base64Image);
    }

    @Override
    public List<String> selectOption(String selector, String values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, ElementHandle values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, String[] values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, SelectOption values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, ElementHandle[] values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public List<String> selectOption(String selector, SelectOption[] values, SelectOptionOptions options) {
        return Collections.emptyList();
    }

    @Override
    public void setChecked(String selector, boolean checked, SetCheckedOptions options) {

    }

    @Override
    public void setContent(String html, SetContentOptions options) {

    }

    @Override
    public void setDefaultNavigationTimeout(double timeout) {

    }

    @Override
    public void setDefaultTimeout(double timeout) {

    }

    @Override
    public void setExtraHTTPHeaders(Map<String, String> headers) {

    }

    @Override
    public void setInputFiles(String selector, Path files, SetInputFilesOptions options) {

    }

    @Override
    public void setInputFiles(String selector, Path[] files, SetInputFilesOptions options) {

    }

    @Override
    public void setInputFiles(String selector, FilePayload files, SetInputFilesOptions options) {

    }

    @Override
    public void setInputFiles(String selector, FilePayload[] files, SetInputFilesOptions options) {

    }

    @Override
    public void setViewportSize(int width, int height) {

    }

    @Override
    public void tap(String selector, TapOptions options) {

    }

    @Override
    public String textContent(String selector, TextContentOptions options) {
        return "";
    }

    @Override
    public String title() {
        return "Dummy Title"; // Placeholder implementation
    }

    @Override
    public Touchscreen touchscreen() {
        return null;
    }

    @Override
    public void type(String selector, String text, TypeOptions options) {

    }

    @Override
    public void uncheck(String selector, UncheckOptions options) {

    }

    @Override
    public void unrouteAll() {

    }

    @Override
    public void unroute(String url, Consumer<Route> handler) {

    }

    @Override
    public void unroute(Pattern url, Consumer<Route> handler) {

    }

    @Override
    public void unroute(Predicate<String> url, Consumer<Route> handler) {

    }

    @Override
    public String url() {
        return url;
    }

    @Override
    public Video video() {
        return null;
    }

    @Override
    public ViewportSize viewportSize() {
        return null;
    }

    @Override
    public Page waitForClose(WaitForCloseOptions options, Runnable callback) {
        return null;
    }

    @Override
    public ConsoleMessage waitForConsoleMessage(WaitForConsoleMessageOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Download waitForDownload(WaitForDownloadOptions options, Runnable callback) {
        return null;
    }

    @Override
    public FileChooser waitForFileChooser(WaitForFileChooserOptions options, Runnable callback) {
        return null;
    }

    @Override
    public JSHandle waitForFunction(String expression, Object arg, WaitForFunctionOptions options) {
        return null;
    }

    @Override
    public void waitForLoadState(LoadState state, WaitForLoadStateOptions options) {

    }

    @Override
    public Response waitForNavigation(WaitForNavigationOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Page waitForPopup(WaitForPopupOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Request waitForRequest(String urlOrPredicate, WaitForRequestOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Request waitForRequest(Pattern urlOrPredicate, WaitForRequestOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Request waitForRequest(Predicate<Request> urlOrPredicate, WaitForRequestOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Request waitForRequestFinished(WaitForRequestFinishedOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Response waitForResponse(String urlOrPredicate, WaitForResponseOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Response waitForResponse(Pattern urlOrPredicate, WaitForResponseOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Response waitForResponse(Predicate<Response> urlOrPredicate, WaitForResponseOptions options, Runnable callback) {
        return null;
    }

    @Override
    public ElementHandle waitForSelector(String selector, WaitForSelectorOptions options) {
        return null;
    }

    @Override
    public void waitForCondition(BooleanSupplier condition, WaitForConditionOptions options) {

    }

    @Override
    public void waitForTimeout(double timeout) {

    }

    @Override
    public void waitForURL(String url, WaitForURLOptions options) {

    }

    @Override
    public void waitForURL(Pattern url, WaitForURLOptions options) {

    }

    @Override
    public void waitForURL(Predicate<String> url, WaitForURLOptions options) {

    }

    @Override
    public WebSocket waitForWebSocket(WaitForWebSocketOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Worker waitForWorker(WaitForWorkerOptions options, Runnable callback) {
        return null;
    }

    @Override
    public List<Worker> workers() {
        return Collections.emptyList();
    }
}
