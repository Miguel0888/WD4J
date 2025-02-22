package wd4j.impl.playwright;

import wd4j.impl.manager.*;
import wd4j.api.*;
import wd4j.impl.webdriver.command.response.WDBrowsingContextResult;
import wd4j.impl.websocket.WDException;
import wd4j.impl.websocket.WebSocketManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BrowserImpl implements Browser {
    private final BrowserTypeImpl browserType;
    private final Session session;
    private final Process process;
    private final WebSocketManager webSocketManager;
    private final WDBrowserManager browserManager;
    private final WDBrowsingContextManager browsingContextManager;
    private final List<UserContextImpl> userContextImpls = new ArrayList<>();
    private String defaultContextId = "default";

    private WDScriptManager scriptManager;
    private WDNetworkManager networkManager;
    private WDStorageManager storageManager;
    private WDWebExtensionManager webExtensionManager;

    private final List<PageImpl> pages = new ArrayList<>(); // aka. BrowsingContexts / Navigables in WebDriver BiDi

    public BrowserImpl(WebSocketManager webSocketManager, BrowserTypeImpl browserType, Process process) throws ExecutionException, InterruptedException {
        this.webSocketManager = webSocketManager;
        this.browserManager = new WDBrowserManager(webSocketManager);
        this.browsingContextManager = new WDBrowsingContextManager(webSocketManager);

        this.scriptManager = new WDScriptManager(webSocketManager);
        this.networkManager = new WDNetworkManager(webSocketManager);
        this.storageManager = new WDStorageManager(webSocketManager);
        this.webExtensionManager = new WDWebExtensionManager(webSocketManager);
        this.browserType = browserType;
        this.process = process;
        this.session = new Session(this); // ToDo: Add PW Options
        fetchDefaultData();
    }

    // ToDo: May require another UserContext if this is not "default"
    private void fetchDefaultData() {
        System.out.println("-----------------------------------");
        System.out.println("Available UserContexts:");
        fetchDefaultSessionData();
        System.out.println("-----------------------------------");
        System.out.println("Available BrowsingContexts:");
        fetchDefaultBrowsingContexts(pages, defaultContextId);
        System.out.println("-----------------------------------");
        // ToDo: Fetch all Pages from every UserContext except the default one
    }

    private void fetchDefaultSessionData() {
        // Get all user contexts already available
        try {
            browserManager.getUserContexts().getUserContexts().forEach(context -> {
                System.out.println("UserContext: " + context.getUserContext().value());
                UserContextImpl uc = new UserContextImpl(this, context.getUserContext().value());
//                fetchDefaultBrowsingContexts(uc.getPages(), context.getUserContext().value());
                userContextImpls.add(uc);
            });
        } catch (WDException ignored) {}
    }

    private void fetchDefaultBrowsingContexts(List<PageImpl> currentPages, String userContextId) {
        // Get all browsing contexts (pages / tabs) already available
        try {
            // Check if a context is already available
            WDBrowsingContextResult.GetTreeResult tree = browsingContextManager.getTree();
            tree.getContexts().forEach(context -> {
                System.out.println("BrowsingContext: " + context.getContext().value());
                currentPages.add(new PageImpl(this, context.getContext().value()));
            });
        } catch (WDException ignored) {}
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void terminateProcess() {
        if (process != null && process.isAlive()) {
            System.out.println("Beende Browser-Prozess...");
            process.destroy(); // Normaler Stop-Versuch
            try {
                if (!process.waitFor(3, TimeUnit.SECONDS)) {
                    System.out.println("Erzwungener Stopp des Browsers...");
                    process.destroyForcibly(); // Erzwinge den Stopp
                }
            } catch (InterruptedException e) {
                System.err.println("Fehler beim Beenden des Browsers: " + e.getMessage());
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Method Overrides
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onDisconnected(Consumer<Browser> handler) {

    }

    @Override
    public void offDisconnected(Consumer<Browser> handler) {

    }

    @Override
    public BrowserType browserType() {
        return browserType;
    }

    @Override
    public void close(CloseOptions options) {
        // ToDo: Implement options
        // ToDo: Implement Cleanup
        terminateProcess();
    }

    @Override
    public List<BrowserContext> contexts() {
        return Collections.unmodifiableList(userContextImpls);
    }

    @Override
    public boolean isConnected() {
        return webSocketManager.isConnected();
    }

    @Override
    public CDPSession newBrowserCDPSession() {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public BrowserContext newContext(NewContextOptions options) {
        UserContextImpl userContext = new UserContextImpl(this);
        userContextImpls.add(userContext);
        return userContext;
    }

    /**
     * Creates a new page in a new browser context. Closing this page will close the context as well.
     *
     * @param options The options for the new page.
     */
    @Override
    public Page newPage(NewPageOptions options) {
        PageImpl page = new PageImpl(this);
        pages.add(page);
        return page;
    }

    @Override
    public void startTracing(Page page, StartTracingOptions options) {

    }

    @Override
    public byte[] stopTracing() {
        return new byte[0];
    }

    @Override
    public String version() {
        return "";
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Compatibility with WebDriver BiDi
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ToDo: Think about where to put the service instances
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public WebSocketManager getWebSockatManager() {
        return webSocketManager;
    }

    public Session getSession() {
        return session;
    }

    /**
     * Returns the BrowserService.
     *
     * @return The BrowserService.
     */
    public WDBrowserManager getBrowserManager() {
        return browserManager;
    }

    public WDBrowsingContextManager getBrowsingContextManager() {
        return browsingContextManager;
    }

    public WDScriptManager getScriptManager() {
        return scriptManager;
    }

    public WDNetworkManager getNetworkManager() {
        return networkManager;
    }

    public WDStorageManager getStorageManager() {
        return storageManager;
    }

    public WDWebExtensionManager getWebExtensionManager() {
        return webExtensionManager;
    }

    public WebSocketManager getWebSocketManager() {
        return webSocketManager;
    }
}
