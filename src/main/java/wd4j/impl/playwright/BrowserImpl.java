package wd4j.impl.playwright;

import wd4j.impl.manager.*;
import wd4j.api.*;
import wd4j.impl.websocket.CommunicationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class BrowserImpl implements Browser {
    private final BrowserTypeImpl browserType;
    private final CommunicationManager communicationManager;
    private final BrowserManager browserManager;
    private final List<BrowserSessionImpl> sessions = new ArrayList<>();
    private String defaultContextId;

    private ScriptManager scriptManager;
    private NetworkManager networkManager;
    private StorageManager storageManager;
    private WebExtensionManager webExtensionManager;

    public BrowserImpl(CommunicationManager communicationManager, BrowserTypeImpl browserType) throws ExecutionException, InterruptedException {
        this.communicationManager = communicationManager;
        this.browserManager = new BrowserManager(communicationManager);

        this.scriptManager = new ScriptManager(communicationManager);
        this.networkManager = new NetworkManager(communicationManager);
        this.storageManager = new StorageManager(communicationManager);
        this.webExtensionManager = new WebExtensionManager(communicationManager);
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
        return communicationManager.isConnected();
    }

    @Override
    public CDPSession newBrowserCDPSession() {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public BrowserContext newContext(NewContextOptions options) {
        BrowserSessionImpl session = new BrowserSessionImpl(communicationManager, this, options);
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
        context = new BrowserSessionImpl(communicationManager, this, null);
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
    public BrowserManager getBrowserManager() {
        return browserManager;
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    public NetworkManager getNetworkManager() {
        return networkManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public WebExtensionManager getWebExtensionManager() {
        return webExtensionManager;
    }
}
