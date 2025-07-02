package de.bund.zrb;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.BindingCallback;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.FunctionCallback;
import com.microsoft.playwright.options.Geolocation;
import de.bund.zrb.support.Pages;
import de.bund.zrb.type.browser.WDUserContext;
import de.bund.zrb.type.browser.WDUserContextInfo;

import java.nio.file.Path;
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
public class UserContextImpl implements BrowserContext {
    private final Pages pages; // aka. BrowsingContexts / Navigables in WebDriver BiDi
    private final BrowserImpl browser;
    private boolean isClosed = false; // ToDo: Is this variable really necessary?

    private final WDUserContext userContext; // ToDo: If found, it should be used to create a new page with this id

    public UserContextImpl(BrowserImpl browser) {
        this.browser = browser;

        this.pages = new Pages(browser, this);

        WDUserContextInfo info = browser.getWebDriver().browser().createUserContext();
        userContext = info.getUserContext();
    }

    public UserContextImpl(BrowserImpl browser, WDUserContext userContext) {
        this.browser = browser;

        this.pages = new Pages(browser, this);

        this.userContext = userContext;
    }

    @Override
    public Page newPage() {
        if (isClosed) {
            throw new PlaywrightException("BrowserContext is closed");
        }
        PageImpl page = new PageImpl(browser, this.userContext); // <-- Hier
        pages.add(page);

        // Beim Schließen wieder austragen:
        page.onClose((e) -> {
            pages.remove(page.getBrowsingContextId());
            System.out.println("🔒 Removed closed Page: " + page.getBrowsingContextId());
        });

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
        pages.onCreated(handler);
    }

    @Override
    public void offPage(Consumer<Page> handler) {
        pages.offCreated(handler);
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
        // ToDo: Implement this; you can use the WDScriptManager.addPreloadScript method, see pageImpl
        //  https://w3c.github.io/webdriver-bidi/#command-script-addPreloadScript
        //  instead of the browserContext just deliver a userContext
    }

    @Override
    public void addInitScript(Path script) {
        // ToDo: Implement this; you can use the WDScriptManager.addPreloadScript method, see pageImpl
        //  https://w3c.github.io/webdriver-bidi/#command-script-addPreloadScript
        //  instead of the browserContext just deliver a userContext
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
        return (List<Page>) pages.asList();
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

    public WDUserContext getUserContext() {
        return userContext;
    }
}
