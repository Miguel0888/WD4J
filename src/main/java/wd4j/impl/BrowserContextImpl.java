package wd4j.impl;

import wd4j.api.options.BindingCallback;
import wd4j.api.options.Cookie;
import wd4j.api.options.FunctionCallback;
import wd4j.api.options.Geolocation;
import wd4j.impl.module.BrowsingContextService;
import wd4j.api.*;

import java.nio.file.Path;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class BrowserContextImpl implements BrowserContext {
    private final BrowsingContextService browsingContextService;
    private final BrowserImpl browser;
    private final List<PageImpl> pages;
    private boolean isClosed;

    private String contextId; // Speichert die Standard-Kontext-ID, falls vorhanden

    public BrowserContextImpl(BrowserImpl browser) {
        // ToDo: Should we pass WebSocketConnection to a service? (BrowserType offers connect() -> where to move it?)
        browsingContextService = browser.getBrowsingContextService();
        this.browser = browser;
        this.pages = new ArrayList<>();
        this.isClosed = false;

        this.contextId = browsingContextService.create();
    }

    public BrowserContextImpl(BrowserImpl browser, String contextId) {
        // ToDo: Should we pass WebSocketConnection to a service? (BrowserType offers connect() -> where to move it?)
        browsingContextService = browser.getBrowsingContextService();
        this.browser = browser;
        this.pages = new ArrayList<>();
        this.isClosed = false;

        this.contextId = contextId;
    }

    @Override
    public Page newPage() {
        if (isClosed) {
            throw new PlaywrightException("BrowserContext is closed");
        }
        PageImpl page = new PageImpl(this);
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

    public String getContextId() {
        return contextId;
    }

    public BrowserImpl getBrowser() {
        return browser;
    }

}