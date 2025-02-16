package wd4j.impl.playwright;

import wd4j.api.options.BindingCallback;
import wd4j.api.options.Cookie;
import wd4j.api.options.FunctionCallback;
import wd4j.api.options.Geolocation;
import wd4j.api.*;
import wd4j.impl.manager.WDSessionManager;
import wd4j.impl.support.EventDispatcher;
import wd4j.impl.webdriver.command.response.WDSessionResult;
import wd4j.impl.websocket.WebSocketManager;
import wd4j.override.BrowserSession;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 *  Differs from the W3C BrowsingContext Module in that it includes the Session Module. (The reason for this may be that
 *  the Chromium DevTools Protocol has a different understanding of what a Context and what a Session is.)
 *  See: https://playwright.dev/java/docs/api/class-browsercontext
 *
 *  Probably, the BrowserContext (PlayWright term) is the Session (corresponding W3C term)
 *  and the Page (PlayWright term) is the Context (corresponding W3C term).
 *
 *  => Better use the terms Session and Page to avoid confusion.
 */
public class BrowserSessionImpl implements BrowserSession {
    //ToDo: Move support.BrowserSession Class in here..
    private final WebSocketManager webSocketManager;
    private final WDSessionManager WDSessionManager;
    private final BrowserImpl browser;
    private final EventDispatcher dispatcher = new EventDispatcher();
    private final List<PageImpl> pages = new ArrayList<>(); // aka. contexts in WebDriver BiDi
    private boolean isClosed = false; // ToDo: Is this variable really necessary?

    private String sessionId;
    private String defaultContextId; // ToDo: If found, it should be used to create a new page with this id

    public BrowserSessionImpl(WebSocketManager webSocketManager, BrowserImpl browser) {
        this(webSocketManager, browser, null);
    }

    public BrowserSessionImpl(WebSocketManager webSocketManager, BrowserImpl browser, Browser.NewContextOptions options) {
        this.webSocketManager = webSocketManager;
        this.browser = browser;

        String browserName = browser.browserType().name();

        if (options != null) {
            // ToDo: Use options to create a new session
        }

        try {
            this.WDSessionManager = new WDSessionManager(webSocketManager);
            fetchDefaultSession(browserName);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Page newPage() {
        if (isClosed) {
            throw new PlaywrightException("BrowserContext is closed");
        }
        PageImpl page = new PageImpl(webSocketManager, this);
        pages.add(page);
        return page;
    }

    @Override
    public void onBackgroundPage(Consumer<Page> handler) {

    }

    @Override
    public void offBackgroundPage(Consumer<Page> handler) {

    }

    @Override
    public void onClose(Consumer<BrowserContext> handler) {

    }

    @Override
    public void offClose(Consumer<BrowserContext> handler) {

    }

    @Override
    public void onConsoleMessage(Consumer<ConsoleMessage> handler) {

    }

    @Override
    public void offConsoleMessage(Consumer<ConsoleMessage> handler) {

    }

    @Override
    public void onDialog(Consumer<Dialog> handler) {

    }

    @Override
    public void offDialog(Consumer<Dialog> handler) {

    }

    @Override
    public void onPage(Consumer<Page> handler) {

    }

    @Override
    public void offPage(Consumer<Page> handler) {

    }

    @Override
    public void onWebError(Consumer<WebError> handler) {

    }

    @Override
    public void offWebError(Consumer<WebError> handler) {

    }

    @Override
    public void onRequest(Consumer<Request> handler) {

    }

    @Override
    public void offRequest(Consumer<Request> handler) {

    }

    @Override
    public void onRequestFailed(Consumer<Request> handler) {

    }

    @Override
    public void offRequestFailed(Consumer<Request> handler) {

    }

    @Override
    public void onRequestFinished(Consumer<Request> handler) {

    }

    @Override
    public void offRequestFinished(Consumer<Request> handler) {

    }

    @Override
    public void onResponse(Consumer<Response> handler) {

    }

    @Override
    public void offResponse(Consumer<Response> handler) {

    }

    @Override
    public Clock clock() {
        return null;
    }

    @Override
    public void addCookies(List<Cookie> cookies) {

    }

    @Override
    public void addInitScript(String script) {

    }

    @Override
    public void addInitScript(Path script) {

    }

    @Override
    public List<Page> backgroundPages() {
        return Collections.emptyList();
    }

    @Override
    public Browser browser() {
        return browser;
    }

    @Override
    public void clearCookies(ClearCookiesOptions options) {

    }

    @Override
    public void clearPermissions() {

    }

    @Override
    public void close() {
        if (!isClosed) {
            for (PageImpl page : pages) {
                page.close();
            }
            pages.clear();
            isClosed = true;
        }
        // ToDo: Should be removed from browser's contexts List ??
    }

    @Override
    public void close(CloseOptions options) {

    }

    @Override
    public List<Cookie> cookies(String urls) {
        return Collections.emptyList();
    }

    @Override
    public List<Cookie> cookies(List<String> urls) {
        return Collections.emptyList();
    }

    @Override
    public void exposeBinding(String name, BindingCallback callback, ExposeBindingOptions options) {

    }

    @Override
    public void exposeFunction(String name, FunctionCallback callback) {

    }

    @Override
    public void grantPermissions(List<String> permissions, GrantPermissionsOptions options) {

    }

    @Override
    public CDPSession newCDPSession(Page page) {
        return null;
    }

    @Override
    public CDPSession newCDPSession(Frame page) {
        return null;
    }

    @Override
    public List<Page> pages() {
        return new ArrayList<>(pages);
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
    public void setDefaultNavigationTimeout(double timeout) {

    }

    @Override
    public void setDefaultTimeout(double timeout) {

    }

    @Override
    public void setExtraHTTPHeaders(Map<String, String> headers) {

    }

    @Override
    public void setGeolocation(Geolocation geolocation) {

    }

    @Override
    public void setOffline(boolean offline) {

    }

    @Override
    public String storageState(StorageStateOptions options) {
        return "";
    }

    @Override
    public Tracing tracing() {
        return null;
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
    public void waitForCondition(BooleanSupplier condition, WaitForConditionOptions options) {

    }

    @Override
    public ConsoleMessage waitForConsoleMessage(WaitForConsoleMessageOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Page waitForPage(WaitForPageOptions options, Runnable callback) {
        return null;
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public BrowserImpl getBrowser() {
        return browser;
    }

    public WDSessionManager getSessionManager() {
        return WDSessionManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public <T> void addEventListener(String eventName, Consumer<T> handler, Class<T> eventClass, WDSessionManager WDSessionManager) {
        dispatcher.addEventListener(eventName, handler, eventClass, WDSessionManager);
    }

    public <T> void removeEventListener(String eventType, Consumer<T> listener, WDSessionManager WDSessionManager) {
        dispatcher.removeEventListener(eventType, listener, WDSessionManager);
    }

    /**
     * Creates a new session aka. browsing context.
     * The default contextId is not provided by every browser.
     * E.g. Firefox ESR does not provide a default contextId, whereas the normal Firefox does.
     *
     * To avoid this issue, you can also create a new context every time you launch a browser. Thus, this method is optional.
     */
    private void fetchDefaultSession(String browserName) throws InterruptedException, ExecutionException {
        // Create a new session
        WDSessionResult.NewWDSessionResult sessionResponse = WDSessionManager.newSession(browserName); // ToDo: Does not work with Chrome!

        // Kontext-ID extrahieren oder neuen Kontext erstellen
        sessionId = processSessionResponse(sessionResponse);

        if(defaultContextId != null) {
            // ToDo: Create Page
            System.out.println("Context ID: " + defaultContextId);
        }
    }

    private String processSessionResponse(WDSessionResult.NewWDSessionResult sessionResponse) {
        if (sessionResponse == null) {
            throw new IllegalArgumentException("SessionResponse darf nicht null sein!");
        }

        // 1️⃣ **Session-ID extrahieren**
        String sessionId = sessionResponse.getSessionId();
        if (sessionId != null) {
            System.out.println("--- Session-ID gefunden: " + sessionId);
        } else {
            System.out.println("⚠ Keine Session-ID gefunden!");
        }

        // 2️⃣ **Default Browsing-Kontext prüfen (Optional)**
        // ⚠ Derzeit ist in der Spezifikation KEIN "contexts"-Feld vorgesehen. Muss ggf. zum DTO hinzugefügt werden!

        return sessionId;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}