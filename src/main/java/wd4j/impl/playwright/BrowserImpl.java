package wd4j.impl.playwright;

import wd4j.impl.manager.*;
import wd4j.api.*;
import wd4j.impl.websocket.WebSocketManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class BrowserImpl implements Browser {
    private final BrowserTypeImpl browserType;
    private final WebSocketManager webSocketManager;
    private final WDBrowserManager WDBrowserManager;
    private final List<BrowserSessionImpl> sessions = new ArrayList<>();
    private String defaultContextId;

    private WDScriptManager WDScriptManager;
    private WDNetworkManager WDNetworkManager;
    private WDStorageManager WDStorageManager;
    private WDWebExtensionManager WDWebExtensionManager;

    public BrowserImpl(WebSocketManager webSocketManager, BrowserTypeImpl browserType) throws ExecutionException, InterruptedException {
        this.webSocketManager = webSocketManager;
        this.WDBrowserManager = new WDBrowserManager(webSocketManager);

        this.WDScriptManager = new WDScriptManager(webSocketManager);
        this.WDNetworkManager = new WDNetworkManager(webSocketManager);
        this.WDStorageManager = new WDStorageManager(webSocketManager);
        this.WDWebExtensionManager = new WDWebExtensionManager(webSocketManager);
        this.browserType = browserType;
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
        // ToDo: implement
    }

    @Override
    public List<BrowserContext> contexts() {
        return Collections.unmodifiableList(sessions);
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
        BrowserSessionImpl session = new BrowserSessionImpl(webSocketManager, this, options);
        sessions.add(session);
        return session;
    }

    /**
     * Creates a new page in a new browser context. Closing this page will close the context as well.
     *
     * @param options The options for the new page.
     */
    @Override
    public Page newPage(NewPageOptions options) {
        BrowserSessionImpl context;
        context = new BrowserSessionImpl(webSocketManager, this, null);
        sessions.add(context);

        return context.newPage();
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

    /**
     * Returns the BrowserService.
     *
     * @return The BrowserService.
     */
    public WDBrowserManager getBrowserManager() {
        return WDBrowserManager;
    }

    public WDScriptManager getScriptManager() {
        return WDScriptManager;
    }

    public WDNetworkManager getNetworkManager() {
        return WDNetworkManager;
    }

    public WDStorageManager getStorageManager() {
        return WDStorageManager;
    }

    public WDWebExtensionManager getWebExtensionManager() {
        return WDWebExtensionManager;
    }
}
