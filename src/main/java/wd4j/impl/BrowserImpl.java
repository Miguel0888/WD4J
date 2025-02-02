package wd4j.impl;

import wd4j.impl.support.BrowserSession;
import wd4j.impl.support.WebSocketDispatcher;
import wd4j.impl.module.BrowserService;
import wd4j.impl.module.BrowsingContextService;
import wd4j.impl.module.SessionService;
import wd4j.impl.module.event.NetworkEvent;
import wd4j.api.*;

import java.util.ArrayList;
import java.util.Arrays;
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

    private final List<BrowserContextImpl> contexts = new ArrayList<>();

    private BrowserSession session;
    private String defaultContextId;
    private WebSocketDispatcher webSocketDispatcher; // Event System

    public BrowserImpl(BrowserTypeImpl browserType) throws ExecutionException, InterruptedException {
        WebSocketImpl webSocketImpl = browserType.getWebSocketConnection();
        this.browserService = new BrowserService(webSocketImpl);
        this.sessionService = new SessionService(webSocketImpl);
        this.browsingContextService = new BrowsingContextService(webSocketImpl);
        this.browserType = browserType;
        this.options = null;

        defaultContextId = initSession();
        if(defaultContextId != null) {
            // Default BrowsingContext zugreifbar machen
            contexts.add(new BrowserContextImpl(this, defaultContextId));
            System.out.println("Default BrowsingContext ID: " + defaultContextId);
        }
        else { //Optional: Create new browser context!
            System.out.println("No default context found.");
//            newContext(new NewContextOptions());
        }

        initEventSystem();
    }

    public BrowserImpl(BrowserTypeImpl browserType, BrowserType.LaunchOptions options) {
        throw new UnsupportedOperationException("Not implemented!");
    }

    ///////////////////////////////////////////////////////////////////////////

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
    private void initEventSystem() {

    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

// Obviously requires a contextId, so it's not possible to fetch the default context from the tree
//    // Fallback-Methode: Kontext über getTree suchen
//    private String fetchDefaultContextFromTree() {
//        WebSocketConnection webSocketConnection = browserType.getWebSocketConnection();
//
//        // ToDo: Use browsingContextService.getTree() and receive() (for blocking) instead!
//        CommandImpl<BrowsingContextService.GetTreeCommand.ParamsImpl> getTreeCommand = new BrowsingContextService.GetTreeCommand();
//
//        try {
//            browsingContextService.getTree();
//            String response = webSocketConnection.send(getTreeCommand);
//
//            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
//            JsonObject result = jsonResponse.getAsJsonObject("result");
//
//            if (result != null && result.has("contexts")) {
//                return result.getAsJsonArray("contexts")
//                        .get(0)
//                        .getAsJsonObject()
//                        .get("context")
//                        .getAsString();
//            }
//        } catch (RuntimeException e) {
//            System.out.println("Error fetching context tree: " + e.getMessage());
//            throw e;
//        }
//
//        return null;
//    }

    ///////////////////////////////////////////////////////////////////////////

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
        return browserType.getWebSocketConnection().isConnected();
    }

    @Override
    public CDPSession newBrowserCDPSession() {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public BrowserContext newContext(NewContextOptions options) {
        // ToDo: Use options
        BrowserContextImpl context = new BrowserContextImpl(this);
        contexts.add(context);
        return context;
    }

    @Override
    public Page newPage(NewPageOptions options) {
        BrowserContextImpl context;
        if (contexts.isEmpty()) {
            context = new BrowserContextImpl(this);
            contexts.add(context);
        } else {
            context = (BrowserContextImpl) contexts.get(0);
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
}
