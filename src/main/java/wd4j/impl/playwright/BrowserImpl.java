package wd4j.impl.playwright;

import wd4j.impl.manager.*;
import wd4j.api.*;
import wd4j.impl.support.Pages;
import wd4j.impl.support.ScriptHelper;
import wd4j.impl.webdriver.command.response.WDBrowsingContextResult;
import wd4j.impl.webdriver.command.response.WDScriptResult;
import wd4j.impl.webdriver.type.browsingContext.WDBrowsingContext;
import wd4j.impl.webdriver.type.script.WDChannel;
import wd4j.impl.webdriver.type.script.WDChannelValue;
import wd4j.impl.webdriver.type.script.WDPrimitiveProtocolValue;
import wd4j.impl.webdriver.type.script.WDRemoteValue;
import wd4j.impl.websocket.WDException;
import wd4j.impl.websocket.WebSocketManager;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BrowserImpl implements Browser {
    private static final List<BrowserImpl> browsers = new ArrayList<>(); // ToDo: Improve this
    private final List<WDScriptResult.AddPreloadScriptResult> globalScripts = new ArrayList<>();
    private final Pages pages = new Pages(); // aka. BrowsingContexts / Navigables in WebDriver BiDi

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

    public BrowserImpl(BrowserTypeImpl browserType, Process process) throws ExecutionException, InterruptedException {
        browsers.add(this);
        this.webSocketManager = WebSocketManager.getInstance();
        this.browserManager = WDBrowserManager.getInstance();
        this.browsingContextManager = WDBrowsingContextManager.getInstance();

        this.scriptManager = WDScriptManager.getInstance();
        this.networkManager = WDNetworkManager.getInstance();
        this.storageManager = WDStorageManager.getInstance();
        this.webExtensionManager = WDWebExtensionManager.getInstance();
        this.browserType = browserType;
        this.process = process;

        this.session = new Session(this); // ToDo: Add PW Options
        onContextSwitch(pages::setActivePageId);
        fetchDefaultData();

        loadGlobalScripts(); // load JavaScript code relevant for the working Playwright API
    }

    private void loadGlobalScripts() {
//        // Channel für Callback anlegen
//        String channelId = UUID.randomUUID().toString(); // Zufällige ID für den Channel
//        WDChannelValue channel = new WDChannelValue(new WDChannelValue.ChannelProperties(new WDChannel(channelId)));
//        String callbackScript = ScriptHelper.loadScript("scripts/callback.js")
//                .replace("<CHANNEL_ID>", channelId);
//        // Callback-Script für die Kommunikation mit dem Playwright-Server (über Message Events)
//        globalScripts.add(scriptManager.addPreloadScript(callbackScript, Collections.singletonList(channel)));


        // 🔹 1️⃣ Channel für das Fokus-Tracking anlegen
        String focusChannelId = "focus-events-channel";  // Feste ID für Fokus-Events
        WDChannelValue focusChannel = new WDChannelValue(new WDChannelValue.ChannelProperties(new WDChannel(focusChannelId)));

        // 🔹 2️⃣ Fokus-Tracking PreloadScript registrieren
        globalScripts.add(scriptManager.addPreloadScript(
                ScriptHelper.loadScript("scripts/focusTracker.js"),
                Collections.singletonList(focusChannel)  // Channel mit übergeben
        ));


        // Alle weiteren globalen Scripts
        globalScripts.add(scriptManager.addPreloadScript(ScriptHelper.loadScript("scripts/events.js")));
        globalScripts.add(scriptManager.addPreloadScript(ScriptHelper.loadScript("scripts/callback.js")));
        globalScripts.add(scriptManager.addPreloadScript(ScriptHelper.loadScript("scripts/dragAndDrop.js")));
    }

    public void removeGlobalScripts() {
        for (WDScriptResult.AddPreloadScriptResult result : globalScripts) {
            scriptManager.removePreloadScript(result.getScript().value());
        }
        globalScripts.clear();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static List<BrowserImpl> getBrowsers() {
        return Collections.unmodifiableList(browsers);
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

    private void fetchDefaultBrowsingContexts(Pages currentPages, String userContextId) {
        // Get all browsing contexts (pages / tabs) already available
        try {
            // Check if a context is already available
            WDBrowsingContextResult.GetTreeResult tree = browsingContextManager.getTree();
            tree.getContexts().forEach(context -> {
                System.out.println("BrowsingContext: " + context.getContext().value());
                currentPages.put(context.getContext().value(),
                        new PageImpl(this, null, context.getContext()));

                // NOT WORKING
//                // ToDo: Find a solution for all preloaded scripts, without duplicated code
//                // 🔹 Falls der Tab existierte, muss das Preload-Skript nachträglich gesetzt werden
//                // 🔹 1️⃣ Channel für das Fokus-Tracking anlegen
//                String focusChannelId = "focus-events-channel";  // Feste ID für Fokus-Events
//                WDChannelValue focusChannel = new WDChannelValue(new WDChannelValue.ChannelProperties(new WDChannel(focusChannelId)));
//                // 🔹 2️⃣ Fokus-Tracking PreloadScript registrieren
//                scriptManager.addPreloadScript(
//                        ScriptHelper.loadScript("scripts/focusTracker.js"),
//                        Collections.singletonList(focusChannel),  // Channel mit übergeben
//                        Collections.singletonList(context.getContext())
//                );
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
        synchronized (pages) { // Sperrt alle Zugriffe auf pages während der Erzeugung einer neuen ContextId
            PageImpl page = new PageImpl(this);
            page.onClose((e) -> {
                pages.remove(page.getBrowsingContextId());
            });
            pages.put(page.getBrowsingContextId(), page);
            return page;
        }
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

    public Pages getPages() {
            return pages;
    }

    public List<UserContextImpl> getUserContextImpls() {
        return userContextImpls;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // ToDo: Es ist nicht ganz sicher, dass es hier keine Race-Conditions gibt
    public static PageImpl getPage(WDBrowsingContext context) {
        return BrowserImpl.getBrowsers().stream()
                .map(browser -> browser.getPages().get(context.value()))  // 🔹 Direkter Map-Access (O(1))
                .filter(Objects::nonNull) // Falls null, überspringen
                .findFirst()
                .orElse(null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void onContextSwitch(Consumer<String> handler) {
        if (handler != null) {
            session.onMessage(message -> {
                if ("focus-events-channel".equals(message.getParams().getChannel().value())) {
                    WDRemoteValue.ObjectRemoteValue remoteValue = (WDRemoteValue.ObjectRemoteValue) message.getParams().getData();

                    boolean isFocusEvent = remoteValue.getValue().entrySet().stream()
                            .filter(entry -> entry.getKey() instanceof WDPrimitiveProtocolValue.StringValue)
                            .filter(entry -> "type".equals(((WDPrimitiveProtocolValue.StringValue) entry.getKey()).getValue()))
                            .map(Map.Entry::getValue)
                            .filter(value -> value instanceof WDPrimitiveProtocolValue.StringValue)
                            .map(value -> ((WDPrimitiveProtocolValue.StringValue) value).getValue())
                            .anyMatch("focus"::equals); // 🔹 Prüft, ob der Wert "focus" ist

                    if (isFocusEvent) {
                        String contextId = message.getParams().getSource().getContext().value();
                        handler.accept(contextId);
                    }
                }
            });
        }
    }
}
