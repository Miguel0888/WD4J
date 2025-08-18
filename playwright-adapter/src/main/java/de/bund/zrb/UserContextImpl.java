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
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class UserContextImpl implements BrowserContext {
    private final Pages pages;
    private final BrowserImpl browser;
    private boolean isClosed = false;

    private final WDUserContext userContext;
    private double defaultTimeout = 30_000; // ms

    // ===== FIX: correct the generic types on the maps (Consumer<Request|Response> as key) =====
    private final Map<Consumer<Request>,  Consumer<Page>> requestWires          = new HashMap<Consumer<Request>,  Consumer<Page>>();
    private final Map<Consumer<Response>, Consumer<Page>> responseWires         = new HashMap<Consumer<Response>, Consumer<Page>>();
    private final Map<Consumer<Request>,  Consumer<Page>> requestFinishedWires  = new HashMap<Consumer<Request>,  Consumer<Page>>();
    private final Map<Consumer<Request>,  Consumer<Page>> requestFailedWires    = new HashMap<Consumer<Request>,  Consumer<Page>>();

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
        if (isClosed) throw new PlaywrightException("BrowserContext is closed");
        PageImpl page = new PageImpl(browser, this.userContext);
        pages.add(page);
        page.onClose(new Consumer<Page>() {
            @Override public void accept(Page e) {
                pages.remove(page.getBrowsingContextId());
                System.out.println("🔒 Removed closed Page: " + page.getBrowsingContextId());
            }
        });
        return page;
    }

    // -----------------------------------------------------------------------------------------
    // Context-level request/response APIs implemented by delegating to all pages (existing + new)
    // -----------------------------------------------------------------------------------------

    @Override
    public void onRequest(final Consumer<Request> handler) {
        if (handler == null) return;

        // Wire existing pages
        for (Page p : pages()) p.onRequest(handler);

        // Wire future pages and remember the hook
        final Consumer<Page> pageHook = new Consumer<Page>() {
            @Override public void accept(Page p) { p.onRequest(handler); }
        };
        pages.onCreated(pageHook);
        requestWires.put(handler, pageHook);
    }

    @Override
    public void offRequest(final Consumer<Request> handler) {
        if (handler == null) return;

        // Unwire future pages
        final Consumer<Page> pageHook = requestWires.remove(handler);
        if (pageHook != null) pages.offCreated(pageHook);

        // Unwire existing pages
        for (Page p : pages()) p.offRequest(handler);
    }

    @Override
    public void onResponse(final Consumer<Response> handler) {
        if (handler == null) return;

        for (Page p : pages()) p.onResponse(handler);

        final Consumer<Page> pageHook = new Consumer<Page>() {
            @Override public void accept(Page p) { p.onResponse(handler); }
        };
        pages.onCreated(pageHook);
        responseWires.put(handler, pageHook);
    }

    @Override
    public void offResponse(final Consumer<Response> handler) {
        if (handler == null) return;

        final Consumer<Page> pageHook = responseWires.remove(handler);
        if (pageHook != null) pages.offCreated(pageHook);

        for (Page p : pages()) p.offResponse(handler);
    }

    @Override
    public void onRequestFinished(final Consumer<Request> handler) {
        if (handler == null) return;

        for (Page p : pages()) p.onRequestFinished(handler);

        final Consumer<Page> pageHook = new Consumer<Page>() {
            @Override public void accept(Page p) { p.onRequestFinished(handler); }
        };
        pages.onCreated(pageHook);
        requestFinishedWires.put(handler, pageHook);
    }

    @Override
    public void offRequestFinished(final Consumer<Request> handler) {
        if (handler == null) return;

        final Consumer<Page> pageHook = requestFinishedWires.remove(handler);
        if (pageHook != null) pages.offCreated(pageHook);

        for (Page p : pages()) p.offRequestFinished(handler);
    }

    @Override
    public void onRequestFailed(final Consumer<Request> handler) {
        if (handler == null) return;

        for (Page p : pages()) p.onRequestFailed(handler);

        final Consumer<Page> pageHook = new Consumer<Page>() {
            @Override public void accept(Page p) { p.onRequestFailed(handler); }
        };
        pages.onCreated(pageHook);
        requestFailedWires.put(handler, pageHook);
    }

    @Override
    public void offRequestFailed(final Consumer<Request> handler) {
        if (handler == null) return;

        final Consumer<Page> pageHook = requestFailedWires.remove(handler);
        if (pageHook != null) pages.offCreated(pageHook);

        for (Page p : pages()) p.offRequestFailed(handler);
    }

    // -----------------------------------------------------------------------------------------
    // Rest wie gehabt (unverändert)
    // -----------------------------------------------------------------------------------------

    @Override public void onBackgroundPage(Consumer<Page> handler) { }
    @Override public void offBackgroundPage(Consumer<Page> handler) { }
    @Override public void onClose(Consumer<BrowserContext> handler) { }
    @Override public void offClose(Consumer<BrowserContext> handler) { }
    @Override public void onConsoleMessage(Consumer<ConsoleMessage> handler) { }
    @Override public void offConsoleMessage(Consumer<ConsoleMessage> handler) { }
    @Override public void onDialog(Consumer<Dialog> handler) { }
    @Override public void offDialog(Consumer<Dialog> handler) { }
    @Override public void onPage(Consumer<Page> handler) { pages.onCreated(handler); }
    @Override public void offPage(Consumer<Page> handler) { pages.offCreated(handler); }
    @Override public void onWebError(Consumer<WebError> handler) { }
    @Override public void offWebError(Consumer<WebError> handler) { }

    @Override public Clock clock() { return null; }
    @Override public void addCookies(List<Cookie> cookies) { }
    @Override public void addInitScript(String script) { }
    @Override public void addInitScript(Path script) { }
    @Override public List<Page> backgroundPages() { return Collections.<Page>emptyList(); }
    @Override public Browser browser() { return browser; }
    @Override public void clearCookies(BrowserContext.ClearCookiesOptions options) { }
    @Override public void clearPermissions() { }
    @Override public void close() {
        if (!isClosed) {
            for (PageImpl page : pages) page.close();
            pages.clear();
            isClosed = true;
        }
    }
    @Override public void close(BrowserContext.CloseOptions options) { }
    @Override public List<Cookie> cookies(String urls) { return Collections.<Cookie>emptyList(); }
    @Override public List<Cookie> cookies(List<String> urls) { return Collections.<Cookie>emptyList(); }
    @Override public void exposeBinding(String name, BindingCallback callback, BrowserContext.ExposeBindingOptions options) { }
    @Override public void exposeFunction(String name, FunctionCallback callback) { }
    @Override public void grantPermissions(List<String> permissions, BrowserContext.GrantPermissionsOptions options) { }
    @Override public CDPSession newCDPSession(Page page) { return null; }
    @Override public CDPSession newCDPSession(Frame page) { return null; }
    @Override public List<Page> pages() { return (List<Page>) pages.asList(); }
    @Override public APIRequestContext request() { return null; }
    @Override public void route(String url, Consumer<Route> handler, BrowserContext.RouteOptions options) { }
    @Override public void route(Pattern url, Consumer<Route> handler, BrowserContext.RouteOptions options) { }
    @Override public void route(Predicate<String> url, Consumer<Route> handler, BrowserContext.RouteOptions options) { }
    @Override public void routeFromHAR(Path har, BrowserContext.RouteFromHAROptions options) { }
    @Override public void routeWebSocket(String url, Consumer<WebSocketRoute> handler) { }
    @Override public void routeWebSocket(Pattern url, Consumer<WebSocketRoute> handler) { }
    @Override public void routeWebSocket(Predicate<String> url, Consumer<WebSocketRoute> handler) { }
    @Override public void setDefaultNavigationTimeout(double timeout) { }
    @Override public void setDefaultTimeout(double timeout) { this.defaultTimeout = timeout; }
    @Override public void setExtraHTTPHeaders(Map<String, String> headers) { }
    @Override public void setGeolocation(Geolocation geolocation) { }
    @Override public void setOffline(boolean offline) { }
    @Override public String storageState(BrowserContext.StorageStateOptions options) { return ""; }
    @Override public Tracing tracing() { return null; }
    @Override public void unrouteAll() { }
    @Override public void unroute(String url, Consumer<Route> handler) { }
    @Override public void unroute(Pattern url, Consumer<Route> handler) { }
    @Override public void unroute(Predicate<String> url, Consumer<Route> handler) { }
    @Override public void waitForCondition(BooleanSupplier condition, BrowserContext.WaitForConditionOptions options) { }
    @Override public ConsoleMessage waitForConsoleMessage(BrowserContext.WaitForConsoleMessageOptions options, Runnable callback) { return null; }
    @Override public Page waitForPage(BrowserContext.WaitForPageOptions options, Runnable callback) { return null; }

    public WDUserContext getUserContext() { return userContext; }
    public void register(PageImpl page) { pages.add(page); }
    public double getDefaultTimeout() { return defaultTimeout; }

    // Prüfe, ob dieser UserContext eine Page mit der gegebenen BrowsingContext-ID enthält
    public boolean hasPage(String browsingContextId) {
        // Guard clauses
        if (isClosed) return false;
        if (browsingContextId == null || browsingContextId.length() == 0) return false;

        // Iterate pages in a safe, read-only manner
        // Use the internal iterable to access PageImpl (needed for getBrowsingContextId()).
        for (PageImpl p : pages) {
            // Compare IDs; stop early on match
            if (browsingContextId.equals(p.getBrowsingContextId())) {
                return true;
            }
        }
        return false;
    }

    // Finde PageImpl anhand der BrowsingContext-ID (oder null)
    private PageImpl findPage(String browsingContextId) {
        if (isClosed || browsingContextId == null || browsingContextId.length() == 0) return null;
        for (PageImpl p : pages) {
            if (browsingContextId.equals(p.getBrowsingContextId())) return p;
        }
        return null;
    }
}
