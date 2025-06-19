package de.bund.zrb;

import com.google.gson.JsonObject;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BindingCallback;
import com.microsoft.playwright.options.FunctionCallback;
import de.bund.zrb.type.script.*;
import de.bund.zrb.event.FrameImpl;
import de.bund.zrb.support.PlaywrightResponse;
import de.bund.zrb.support.ScriptHelper;
import de.bund.zrb.command.response.WDBrowsingContextResult;
import de.bund.zrb.command.response.WDScriptResult;
import de.bund.zrb.event.WDBrowsingContextEvent;
import de.bund.zrb.type.browser.WDClientWindow;
import de.bund.zrb.type.browsingContext.WDInfo;
import de.bund.zrb.type.browsingContext.WDLocator;
import de.bund.zrb.websocket.WDEventNames;
import de.bund.zrb.support.JsonToPlaywrightMapper;
import de.bund.zrb.type.browser.WDUserContext;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.session.WDSubscription;
import de.bund.zrb.type.session.WDSubscriptionRequest;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class PageImpl implements Page {
    private final WDBrowsingContext browsingContext; // aka. browsing context or navigable in WebDriver BiDi
    private final WDUserContext userContextId; // aka. simply as contextId in CDP - default is "default"
    private boolean isClosed;
    private String url;

    private final BrowserImpl browser;
    private final WebDriver webDriver;

    // ToDo: Not supported yet, just for testing: Firefox does not remember the id, only accepts event + contextId
    private WDSubscription consoleMessageSubscription;

    private List<WDScriptResult.AddPreloadScriptResult> addPreloadScriptResults = new ArrayList<>();

    /**
     * Constructor for a new page.
     * @param browser
     */
    public PageImpl(BrowserImpl browser) {
        this(browser, null);
    }

    /**
     * Constructor for a new page.
     * @param browser
     * @param userContext
     */
    public PageImpl(BrowserImpl browser, WDUserContext userContext) {
        this.browser = browser;
        this.webDriver = browser.getWebDriver();

        this.isClosed = false;
        this.url = "about:blank"; // Standard-Startseite

        this.browsingContext = new WDBrowsingContext(browser.getWebDriver().browsingContext().create().getContext());
        this.userContextId = userContext;
    }

    /**
     * Constructor for a new page with a given pageId.
     * @param browser
     * @param userContext
     * @param browsingContext
     */
    public PageImpl(BrowserImpl browser,  WDUserContext userContext, WDBrowsingContext browsingContext) {
        this.browser = browser;
        this.webDriver = browser.getWebDriver();

        this.isClosed = false;
        this.url = "about:blank"; // Standard-Startseite

        this.browsingContext = browsingContext;
        this.userContextId = userContext;
    }

    public PageImpl(WDBrowsingContextEvent.Load load) {
        WDBrowsingContext context = load.getParams().getContext();
        PageImpl existingPage = BrowserImpl.getPage(context); // ToDo: No static access to BrowserImpl, find correct browser via the Connection or SessionId

        this.browser = existingPage != null ? ((PageImpl) existingPage).getBrowser() : null;
        this.webDriver = browser.getWebDriver();
        this.userContextId = (existingPage != null) ? existingPage.getUserContext() : null;

        // 🔹 Falls keine existierende Seite vorhanden ist, eine neue Instanz initialisieren
        this.browsingContext = context;
        this.isClosed = false;
        this.url = load.getParams().getUrl();
    }

    public PageImpl(WDBrowsingContextEvent.DomContentLoaded domContentLoaded) {
        WDBrowsingContext context = domContentLoaded.getParams().getContext();
        PageImpl existingPage = BrowserImpl.getPage(context);

        // 🔹 Übernahme der bestehenden Browser-Instanz und Session, falls vorhanden
        this.browser = existingPage != null ? existingPage.getBrowser() : null;
        this.webDriver = browser.getWebDriver();
        this.userContextId = (existingPage != null) ? existingPage.getUserContext() : null;

        // 🔹 Falls keine existierende Seite vorhanden ist, eine neue Instanz initialisieren
        this.browsingContext = context;
        this.isClosed = false;
        this.url = domContentLoaded.getParams().getUrl();
    }

    public PageImpl(WDBrowsingContextEvent.Destroyed destroyed) {
        WDBrowsingContext context = destroyed.getParams().getContext();
        PageImpl existingPage = BrowserImpl.getPage(context);

        // 🔹 Falls die Page existiert, markieren wir sie als geschlossen
        this.browser = existingPage != null ? existingPage.getBrowser() : null;
        this.webDriver = browser.getWebDriver();
        this.userContextId = (existingPage != null) ? existingPage.getUserContext() : null;

        this.browsingContext = context;
        this.isClosed = true;  // Diese Page gilt als "destroyed"
        this.url = (existingPage != null) ? existingPage.url() : null;
    }

    public PageImpl(WDBrowsingContextEvent.Created created) {
        WDBrowsingContext context = created.getParams().getContext();
        PageImpl existingPage = BrowserImpl.getPage(context);

        // 🔹 Falls eine existierende Page vorhanden ist, übernehmen wir ihre fehlenden Werte
        this.browser = existingPage != null ? existingPage.getBrowser() : null;
        this.webDriver = browser.getWebDriver();
        this.userContextId = (existingPage != null) ? existingPage.getUserContext() : created.getParams().getUserContext();

        this.browsingContext = context;
        this.isClosed = false;
        this.url = created.getParams().getUrl();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Standard Features, directly supported by WebDriver
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onClose(Consumer<Page> handler) {
        if (handler != null) {
            WDSubscriptionRequest subscriptionRequest = new WDSubscriptionRequest(WDEventNames.CONTEXT_DESTROYED.getName(), this.getBrowsingContextId(), null);
            webDriver.addEventListener(subscriptionRequest, handler);
        }
    }

    @Override
    public void offClose(Consumer<Page> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.CONTEXT_DESTROYED.getName(), getBrowsingContextId(), handler);
        }
    }

    @Override
    public void onConsoleMessage(Consumer<ConsoleMessage> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.ENTRY_ADDED.getName(), this.getBrowsingContextId(), null);
            consoleMessageSubscription = webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    @Override
    public void offConsoleMessage(Consumer<ConsoleMessage> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.ENTRY_ADDED.getName(), getBrowsingContextId(), handler);
        }
    }

    // ToDo: Not supported yet
//    @Override
//    public void offConsoleMessage(Consumer<ConsoleMessage> handler) {
//        if (consoleMessageSubscription != null) {
//            session.removeEventListener(consoleMessageSubscription, handler);
//            consoleMessageSubscription = null;
//        }
//    }


    @Override
    public void onCrash(Consumer<Page> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.NAVIGATION_FAILED.getName(), this.getBrowsingContextId(), null);
            webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    @Override
    public void offCrash(Consumer<Page> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.NAVIGATION_FAILED.getName(), getBrowsingContextId(), handler);
        }
    }

    @Override
    public void onDialog(Consumer<Dialog> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.USER_PROMPT_OPENED.getName(), this.getBrowsingContextId(), null);
            webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    @Override
    public void offDialog(Consumer<Dialog> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.USER_PROMPT_OPENED.getName(), getBrowsingContextId(), handler);
        }
    }

    @Override
    public void onDOMContentLoaded(Consumer<Page> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.DOM_CONTENT_LOADED.getName(), this.getBrowsingContextId(), null);
            webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    @Override
    public void offDOMContentLoaded(Consumer<Page> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.DOM_CONTENT_LOADED.getName(), getBrowsingContextId(), handler);
        }
    }

    @Override
    public void onLoad(Consumer<Page> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.LOAD.getName(), this.getBrowsingContextId(), null);
            webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    @Override
    public void offLoad(Consumer<Page> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.LOAD.getName(), getBrowsingContextId(), handler);
        }
    }

    @Override
    public void onRequest(Consumer<Request> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.BEFORE_REQUEST_SENT.getName(), this.getBrowsingContextId(), null);
            webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    @Override
    public void offRequest(Consumer<Request> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.BEFORE_REQUEST_SENT.getName(), getBrowsingContextId(), handler);
        }
    }

    @Override
    public void onRequestFailed(Consumer<Request> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.FETCH_ERROR.getName(), this.getBrowsingContextId(), null);
            webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    @Override
    public void offRequestFailed(Consumer<Request> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.FETCH_ERROR.getName(), getBrowsingContextId(), handler);
        }
    }

    @Override
    public void onRequestFinished(Consumer<Request> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.RESPONSE_COMPLETED.getName(), this.getBrowsingContextId(), null);
            webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    @Override
    public void offRequestFinished(Consumer<Request> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.RESPONSE_COMPLETED.getName(), getBrowsingContextId(), handler);
        }
    }

    @Override
    public void onResponse(Consumer<Response> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.RESPONSE_STARTED.getName(), this.getBrowsingContextId(), null);
            webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    @Override
    public void offResponse(Consumer<Response> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.RESPONSE_STARTED.getName(), getBrowsingContextId(), handler);
        }
    }

    @Override
    public void onWebSocket(Consumer<WebSocket> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.CONTEXT_CREATED.getName(), this.getBrowsingContextId(), null);
            webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    @Override
    public void offWebSocket(Consumer<WebSocket> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.CONTEXT_CREATED.getName(), getBrowsingContextId(), handler);
        }
    }

    @Override
    public void onWorker(Consumer<Worker> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.REALM_CREATED.getName(), this.getBrowsingContextId(), null);
            webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    @Override
    public void offWorker(Consumer<Worker> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.REALM_CREATED.getName(), getBrowsingContextId(), handler);
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final List<Consumer<String>> pageErrorListeners = new ArrayList<>();

    @Override
    public void onPageError(Consumer<String> handler) {
        pageErrorListeners.add(handler);
    }

    @Override
    public void offPageError(Consumer<String> handler) {
        pageErrorListeners.remove(handler);
    }

    /**
     * Kann immer benutzt werden, wenn eine exception auf der Seite auftritt, z.B. bei einem evaluate-Script-Befehl.
     *
     * Solche Fehler führen nicht zu einem Java Fehler, da sie als Type "exception" und nicht "error" vom Browser
     * zurückgegeben werden. Außerdem sieht Playwright für den Page Content z.B. auch keine Java Exceptions vor und
     * liefert bestenfalls NULL zurück.
     *
     * @param errorMessage
     */
    private void notifyPageErrorListeners(String errorMessage) {
        for (Consumer<String> listener : pageErrorListeners) {
            listener.accept(errorMessage);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onPopup(Consumer<Page> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.CONTEXT_CREATED.getName(), this.getBrowsingContextId(), null);
            webDriver.addEventListener(wdSubscriptionRequest, jsonObject -> {
                // Stelle sicher, dass jsonObject tatsächlich ein JsonObject ist
                Page popupPage = JsonToPlaywrightMapper.mapToInterface((JsonObject) jsonObject, Page.class);
                handler.accept(popupPage);
            });
        }
    }

    @Override
    public void offPopup(Consumer<Page> handler) {
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.CONTEXT_CREATED.getName(), getBrowsingContextId(), handler);
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
        addPreloadScriptResults.add(((BrowserImpl) browser).getScriptManager().addPreloadScript(
                script, getBrowsingContextId()
        ));
    }

    @Override
    public void addInitScript(Path scriptPath) {
        String scriptData = ScriptHelper.loadScript(scriptPath.toString());
        addPreloadScriptResults.add(((BrowserImpl) browser).getScriptManager().addPreloadScript(
                scriptData, getBrowsingContextId()
        ));
    }

    public void removeInitScripts() {
        for (WDScriptResult.AddPreloadScriptResult result : addPreloadScriptResults) {
            ((BrowserImpl) browser).getScriptManager().removePreloadScript(result.getScript().value());
        }
        addPreloadScriptResults.clear();
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
        isClosed = true;
        browser.getWebDriver().browsingContext().close(getBrowsingContextId());
//        browser.getPages().remove(pageId);
    }

    @Override
    public void close(CloseOptions options) {
        throw new UnsupportedOperationException("Reasons and before Unload Handlers are not supported by WebDriver BiDi");
    }

    @Override
    public String content() {
        if (isClosed) {
            throw new PlaywrightException("Page is closed");
        }

        // Ziel: BrowsingContext für das `evaluate()`-Command setzen
        WDTarget.ContextTarget contextTarget = new WDTarget.ContextTarget(new WDBrowsingContext(getBrowsingContextId()));

        // WebDriver BiDi Evaluate-Command ausführen
        WDEvaluateResult evaluate = webDriver.script().evaluate(
                "return document.documentElement.outerHTML;", contextTarget, true);

        try {
            // ✅ Wenn erfolgreich, den String aus `PrimitiveProtocolValue.StringValue` extrahieren
            return ((WDEvaluateResult.WDEvaluateResultSuccess) evaluate).getResult().asString();
        } catch (ClassCastException e) {
            // ❌ Fehler aufgetreten -> wahrscheinlich WDEvaluateResultError
            WDEvaluateResult.WDEvaluateResultError error = (WDEvaluateResult.WDEvaluateResultError) evaluate;
            notifyPageErrorListeners(error.getExceptionDetails().getText());

            // 🔥 Fehlerbehandlung: Null zurückgeben, falls der HTML-Code nicht ausgelesen werden konnte
            return null;
        }
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
        WDEvaluateResult result;
        WDTarget target = new WDTarget.ContextTarget(browsingContext); // oder RealmTarget

        if (isFunctionExpression(expression)) {
            // Verwende callFunction wenn Argumente vorhanden sind
            List<WDLocalValue> args = arg != null
                    ? Collections.singletonList(WDLocalValue.fromObject(arg))
                    : Collections.emptyList();

            result = webDriver.script().callFunction(
                    expression,
                    true, // awaitPromise
                    target,
                    args,
                    null, // thisObject
                    WDResultOwnership.ROOT,
                    null // serializationOptions
            );
        } else {
            // Normales evaluate ohne Argumente
            result = webDriver.script().evaluate(
                    expression,
                    target,
                    true,
                    WDResultOwnership.ROOT,
                    null // sandbox
            );
        }

        if (result instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue remote = ((WDEvaluateResult.WDEvaluateResultSuccess) result).getResult();

            if (remote instanceof WDRemoteReference.SharedReference) {
                return new JSHandleImpl(webDriver, ((WDRemoteReference.SharedReference) remote), target);
            }
            else if(remote instanceof WDRemoteReference.RemoteObjectReference) { // ToDo: Check if this is correct, should always be a SharedReference!
                return new JSHandleImpl(webDriver, ((WDRemoteReference.RemoteObjectReference) remote), target);
            }
        }

        throw new RuntimeException("evaluateHandle failed: unexpected result type");
    }


    @Override
    public void exposeBinding(String name, BindingCallback callback, ExposeBindingOptions options) {
        String preloadCode =
                "window['" + name + "'] = function(...args) {" +
                        "  return new Promise(function(resolve, reject) {" +
                        "    window.__playwright_invokeBinding('" + name + "', args).then(resolve).catch(reject);" +
                        "  });" +
                        "};";

        webDriver.script().addPreloadScript(
                getBrowsingContextId(),
                preloadCode
        );

        webDriver.registerBinding(name, callback); // Diese Methode bauen wir gleich!
    }

    @Override
    public void exposeFunction(String name, FunctionCallback callback) {
        String preloadCode =
                "window['" + name + "'] = function(...args) {" +
                        "  return new Promise(function(resolve, reject) {" +
                        "    window.__playwright_invoke('" + name + "', args).then(resolve).catch(reject);" +
                        "  });" +
                        "};";

        webDriver.script().addPreloadScript(
                getBrowsingContextId(),
                preloadCode
        );

        webDriver.registerFunction(name, callback); // Diese Methode bauen wir gleich!
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
        List<Frame> frames = new ArrayList<>();
        WDBrowsingContextResult.GetTreeResult tree = webDriver.browsingContext().getTree(browsingContext, Long.MAX_VALUE);
        Collection<WDInfo> infos = tree.getContexts();
        for(WDInfo info : infos) {
            WDBrowsingContext context = info.getContext();
            WDClientWindow clientWindow = info.getClientWindow();
            WDBrowsingContext originalOpener = info.getOriginalOpener();
            String url1 = info.getUrl();
            WDUserContext userContext = info.getUserContext();
            WDBrowsingContext parent = info.getParent();
            Collection<WDInfo> children = info.getChildren();

            if(parent != null) { // ToDo: Check if this is correct (all children frames only?)
                if(browsingContext.equals(parent)) {
                    frames.add(new FrameImpl(this, userContext, clientWindow, url1, children));
                }
                else {
                    throw new PlaywrightException("Parent context does not correspond to the sub frame's context.");
                }
            }
        }
        return frames;
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
        browser.getWebDriver().browsingContext().traverseHistory(getBrowsingContextId(), -1);

        return null; // ToDo: Echte Response zurückgeben
    }

    @Override
    public Response goForward(GoForwardOptions options) {
        browser.getWebDriver().browsingContext().traverseHistory(getBrowsingContextId(), 1);

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
        if (getBrowsingContextId() == null || getBrowsingContextId().isEmpty()) {
            throw new PlaywrightException("Cannot navigate: contextId is null or empty!");
        }
        this.url = url;

        // WebDriver BiDi Befehl senden
        WDBrowsingContextResult.NavigateResult navigate = browser.getWebDriver().browsingContext().navigate(url, getBrowsingContextId());

        return new PlaywrightResponse<WDBrowsingContextResult.NavigateResult>(navigate);
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
        return new LocatorImpl(webDriver, this, selector);
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
        if (selector == null || selector.isEmpty()) {
            throw new IllegalArgumentException("Selector must not be null or empty.");
        }

        String contextTarget = this.getBrowsingContextId();

//        WDLocator locator = selector.trim().startsWith("//") || selector.trim().startsWith("(//")
//                ? new WDLocator.XPathLocator(selector)
//                : new WDLocator.CSSLocator(selector); // Falls CSS
        WDLocator.XPathLocator locator = new WDLocator.XPathLocator(selector); // ToDo: Allow other locators

        WDBrowsingContextResult.LocateNodesResult nodes = browser.getWebDriver().browsingContext().locateNodes(
                contextTarget,
                locator
        );

        if (nodes.getNodes().isEmpty()) {
            System.out.println("No nodes found for selector: " + selector);
            return null;
        }
        else
        {
            WDSharedId sharedId = nodes.getNodes().get(0).getSharedId(); // ToDo: Use Object directly instead of String
            WDHandle handle = nodes.getNodes().get(0).getHandle(); // Falls vorhanden
            WDRemoteReference.SharedReference sharedReference = new WDRemoteReference.SharedReference(sharedId, handle);
            WDTarget.ContextTarget contextTargetObj = new WDTarget.ContextTarget(new WDBrowsingContext(contextTarget));
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Shared ID: " + sharedId + " <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            return new ElementHandleImpl(webDriver, sharedReference, contextTargetObj);
        }
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
        browser.getWebDriver().browsingContext().reload(getBrowsingContextId());

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
        // ToDo: Use options
        WDBrowsingContextResult.CaptureScreenshotResult captureScreenshotResult = browser.getWebDriver().browsingContext().captureScreenshot(getBrowsingContextId());
        String base64Image = captureScreenshotResult.getData();
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

    public WDBrowsingContext getBrowsingContext() {
        return browsingContext;
    }

    public String getBrowsingContextId() {
        if (browsingContext == null) {
            throw new PlaywrightException("Browsing context is null.");
        }
        return browsingContext.value();
    }

    public WDUserContext getUserContext() {
        return userContextId;
    }

    public String getUserContextId() {
        return userContextId.value();
    }

    public BrowserImpl getBrowser() {
        return browser;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Helper Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Prüft, ob der übergebene Ausdruck eine Funktion ist. Wird benötigt, um zu entscheiden, ob ein `callFunction` oder
     * ein `evaluate`-Befehl ausgeführt werden soll.
     *
     * @param expr
     * @return
     */
    private boolean isFunctionExpression(String expr) {
        return expr != null && expr.trim().matches("^\\(?\\s*[^)]*\\)?\\s*=>.*");
    }

    public WebDriver getWebDriver() {
        return webDriver;
    }

}
