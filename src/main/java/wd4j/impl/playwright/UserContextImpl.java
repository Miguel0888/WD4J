package wd4j.impl.playwright;

import wd4j.api.*;
import wd4j.api.options.BindingCallback;
import wd4j.api.options.Cookie;
import wd4j.api.options.FunctionCallback;
import wd4j.api.options.Geolocation;
import wd4j.impl.websocket.WebSocketManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/** !!! WebDriverBiDi vs. CDP Terminology !!! */
// The WebDriverBiDi term "UserContext" is equivalent to the CDP term "BrowserContext".
// IT IS DIFFERENT FROM THE W3C BROWSING CONTEXT MODULE, WHICH IS A NAVIGABLE AKA. PAGE IN CHROMIUM DEVTOOLS PROTOCOL.
public class UserContextImpl implements BrowserContext{
    private final List<PageImpl> pages = new ArrayList<>(); // aka. BrowsingContexts / Navigables in WebDriver BiDi
    private final BrowserImpl browser;
    private final Session session;
    private boolean isClosed = false; // ToDo: Is this variable really necessary?

    private String defaultContextId; // ToDo: If found, it should be used to create a new page with this id
    private WebSocketManager webSocketManager;

    public UserContextImpl(BrowserImpl browser) {
        this.browser = browser;
        this.webSocketManager = browser.getWebSockatManager();
        this.session = browser.getSession();

        // ToDo: Send new WDBrowserRequest#createUserContext command to the browser
    }


    @Override
    public Page newPage() {
        if (isClosed) {
            throw new PlaywrightException("BrowserContext is closed");
        }
        PageImpl page = new PageImpl(webSocketManager, session);
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
    public void clearCookies(BrowserContext.ClearCookiesOptions options) {

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
    public void close(BrowserContext.CloseOptions options) {

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
    public void exposeBinding(String name, BindingCallback callback, BrowserContext.ExposeBindingOptions options) {

    }

    @Override
    public void exposeFunction(String name, FunctionCallback callback) {

    }

    @Override
    public void grantPermissions(List<String> permissions, BrowserContext.GrantPermissionsOptions options) {

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
    public void route(String url, Consumer<Route> handler, BrowserContext.RouteOptions options) {

    }

    @Override
    public void route(Pattern url, Consumer<Route> handler, BrowserContext.RouteOptions options) {

    }

    @Override
    public void route(Predicate<String> url, Consumer<Route> handler, BrowserContext.RouteOptions options) {

    }

    @Override
    public void routeFromHAR(Path har, BrowserContext.RouteFromHAROptions options) {

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
    public String storageState(BrowserContext.StorageStateOptions options) {
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
    public void waitForCondition(BooleanSupplier condition, BrowserContext.WaitForConditionOptions options) {

    }

    @Override
    public ConsoleMessage waitForConsoleMessage(BrowserContext.WaitForConsoleMessageOptions options, Runnable callback) {
        return null;
    }

    @Override
    public Page waitForPage(BrowserContext.WaitForPageOptions options, Runnable callback) {
        return null;
    }
}
