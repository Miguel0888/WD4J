package wd4j.impl.playwright;

import wd4j.impl.service.*;
import wd4j.impl.support.BrowserSession;
import wd4j.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class BrowserImpl implements Browser {
    private final BrowserTypeImpl browserType;  // contains the connection and the command line
    private final BrowserType.LaunchOptions options; // not used yet

    private final BrowserService browserService;
    private final SessionService sessionService;
    private final BrowsingContextService browsingContextService;

    private final List<BrowserSessionImpl> contexts = new ArrayList<>();

    private BrowserSession session;
    private String defaultContextId;
    private WebSocketImpl webSocketImpl;
    private ScriptService scriptService;
    private NetworkService networkService;
    private StorageService storageService;
    private WebExtensionService webExtensionService;

    public BrowserImpl(BrowserTypeImpl browserType, WebSocketImpl webSocketImpl) throws ExecutionException, InterruptedException {
        this.webSocketImpl = webSocketImpl;
        this.browserService = new BrowserService(webSocketImpl);
        this.sessionService = new SessionService(webSocketImpl);
        this.browsingContextService = new BrowsingContextService(webSocketImpl);
        this.scriptService = new ScriptService(webSocketImpl);
        this.networkService = new NetworkService(webSocketImpl);
        this.storageService = new StorageService(webSocketImpl);
        this.webExtensionService = new WebExtensionService(webSocketImpl);
        this.browserType = browserType;
        this.options = null;

        // ToDo: Move the Session (initSession) to the BrowserContextImpl,
        //  see: https://playwright.dev/java/docs/api/class-browsercontext
        defaultContextId = initSession();
        if(defaultContextId != null) {
            // Default BrowsingContext zugreifbar machen
            contexts.add(new BrowserSessionImpl(this, defaultContextId));
            System.out.println("Default BrowsingContext ID: " + defaultContextId);
        }
        else { //Optional: Create new browser context!
            System.out.println("No default context found.");
//            newContext(new NewContextOptions());
        }
    }

    public BrowserImpl(BrowserTypeImpl browserType, BrowserType.LaunchOptions options) {
        throw new UnsupportedOperationException("Not implemented!");
    }

    ///////////////////////////////////////////////////////////////////////////

    /**
     * Required for the WebSocket connection
     * <p>
     * ToDo: Should we pass WebSocketConnection to a service? (BrowserType offers connect() -> where to move it?)
     *  e.g. the BrowserService?
     *
     * @return
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
        return Collections.unmodifiableList(contexts);
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
        // ToDo: Use options
        BrowserSessionImpl context = new BrowserSessionImpl(this);
        contexts.add(context);
        return context;
    }

    @Override
    public Page newPage(NewPageOptions options) {
        BrowserSessionImpl context;
        if (contexts.isEmpty()) {
            context = new BrowserSessionImpl(this);
            contexts.add(context);
        } else {
            context = (BrowserSessionImpl) contexts.get(0);
        }
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
    public BrowserService getBrowserService() {
        return browserService;
    }

    /**
     * Returns the SessionService.
     *
     * @return The SessionService.
     */
    public SessionService getSessionService() {
        return sessionService;
    }

    public BrowsingContextService getBrowsingContextService() {
        return browsingContextService;
    }

    public ScriptService getScriptService() {
        return scriptService;
    }

    public NetworkService getNetworkService() {
        return networkService;
    }

    public StorageService getStorageService() {
        return storageService;
    }

    public WebExtensionService getWebExtensionService() {
        return webExtensionService;
    }
}
