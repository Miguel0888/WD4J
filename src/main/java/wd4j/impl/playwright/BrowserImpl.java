package wd4j.impl.playwright;

import wd4j.impl.manager.*;
import wd4j.impl.support.BrowserSession;
import wd4j.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class BrowserImpl implements Browser {
    private final BrowserTypeImpl browserType;

    private final BrowserManager browserManager;

    private final List<BrowserSessionImpl> sessions = new ArrayList<>();

    private BrowserSession session;
    private String defaultContextId;
    private WebSocketImpl webSocketImpl;
    private ScriptManager scriptManager;
    private NetworkManager networkManager;
    private StorageManager storageManager;
    private WebExtensionManager webExtensionManager;

    public BrowserImpl(BrowserTypeImpl browserType, WebSocketImpl webSocketImpl) throws ExecutionException, InterruptedException {
        this.webSocketImpl = webSocketImpl;
        this.browserManager = new BrowserManager(webSocketImpl);

        this.scriptManager = new ScriptManager(webSocketImpl);
        this.networkManager = new NetworkManager(webSocketImpl);
        this.storageManager = new StorageManager(webSocketImpl);
        this.webExtensionManager = new WebExtensionManager(webSocketImpl);
        this.browserType = browserType;
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Required for the WebSocket connection
     *
     * @return The WebSocket connection.
     */
    public WebSocketImpl getWebSocketConnection() {
        return webSocketImpl;
    }

    private String initSession() throws ExecutionException, InterruptedException {
        String defaultContextId = null;
        session = new BrowserSession(this, browserType.name());

//        if(session.getDefaultContextId() == null) {
//            // Fallback zu browsingContext.getTree, wenn kein Kontext gefunden wurde
//            System.out.println("--- Keine default Context-ID gefunden. Führe browsingContext.getTree aus. ---");
//            defaultContextId = fetchDefaultContextFromTree(); // not working
//        }
        return defaultContextId;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


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
        return webSocketImpl.isConnected();
    }

    @Override
    public CDPSession newBrowserCDPSession() {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public BrowserContext newContext(NewContextOptions options) {
        BrowserSessionImpl session = new BrowserSessionImpl(this, options);
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
        context = new BrowserSessionImpl(this, null);
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

    /**
     * Returns the BrowserSession. In WebDriver BiDi, every BrowserContext requires a BrowserSession first. This is a
     * main difference to the Chromium DevTools Protocol, where BrowserContexts do not depend on a BrowserSession.
     *
     * Maybe this difference between WebDriver BiDi and CDP is the reason, why PlayWright does not offer Sessions in the API.
     * But in WebDriver BiDi, the Session is the main entry point to the BrowserContext and Event Processing. That is
     * why have to use Session under the hood to provide the full PlayWright Feature Set.
     *
     * Nevertheless, the BrowserSession should not be widespread and encapsulated in  BrowserImpl (or BrowserContextImpl
     * if this is more appropriate) for compatibility reasons.
     *
     * Gives Access to the Event System! (subscribe, unsubscribe are part of the Sesseion Module in W3C Spec)
     *
     * @return
     */
    public BrowserSession getSession() {
        return session;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ToDo: Think about where to put the service instances
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the BrowserService.
     *
     * @return The BrowserService.
     */
    public BrowserManager getBrowserService() {
        return browserManager;
    }

    public ScriptManager getScriptService() {
        return scriptManager;
    }

    public NetworkManager getNetworkService() {
        return networkManager;
    }

    public StorageManager getStorageService() {
        return storageManager;
    }

    public WebExtensionManager getWebExtensionService() {
        return webExtensionManager;
    }
}
