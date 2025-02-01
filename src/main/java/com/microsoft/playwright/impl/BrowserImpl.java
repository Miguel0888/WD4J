package com.microsoft.playwright.impl;

import com.microsoft.playwright.*;
import wd4j.core.WebSocketConnection;
import wd4j.impl.module.BrowserService;
import wd4j.impl.module.BrowsingContextService;
import wd4j.impl.module.SessionService;

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

    private final List<BrowserContext> contexts = new ArrayList<>();

    private BrowserSession session;
    private String defaultContextId;

    public BrowserImpl(BrowserTypeImpl browserType) throws ExecutionException, InterruptedException {
        WebSocketConnection webSocketConnection = browserType.getWebSocketConnection();
        this.browserService = new BrowserService(webSocketConnection);
        this.sessionService = new SessionService(webSocketConnection);
        this.browsingContextService = new BrowsingContextService(webSocketConnection);
        this.browserType = browserType;
        this.options = null;

        defaultContextId = initSession();
        if(defaultContextId != null) {
            // BrowsingContext erstellen und speichern
            contexts.add(new BrowserContextImpl(this, defaultContextId));
            System.out.println("Default BrowsingContext ID: " + defaultContextId);
        }
        else { //Optional: Create new browser context!
            System.out.println("No default context found.");
            newContext(new NewContextOptions());
        }
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



    ///////////////////////////////////////////////////////////////////////////

    /**
     * Returns the BrowsingContext at the given index.
     * A BrowsingContext is a tab or window in the browser!
     *
     * @param index The index of the BrowsingContext.
     * @return The BrowsingContext at the given index.
     */
    private BrowserContext getBrowsingContext(int index) {
        return contexts.get(index);
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
