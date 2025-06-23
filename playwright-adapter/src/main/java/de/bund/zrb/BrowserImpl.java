package de.bund.zrb;

import com.microsoft.playwright.*;
import de.bund.zrb.manager.WDScriptManager;
import de.bund.zrb.api.WebSocketManager;
import de.bund.zrb.support.Pages;
import de.bund.zrb.support.ScriptHelper;
import de.bund.zrb.command.response.WDBrowsingContextResult;
import de.bund.zrb.command.response.WDScriptResult;
import de.bund.zrb.websocket.WDEventNames;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.script.WDChannel;
import de.bund.zrb.type.script.WDChannelValue;
import de.bund.zrb.type.script.WDPrimitiveProtocolValue;
import de.bund.zrb.type.script.WDRemoteValue;
import de.bund.zrb.type.session.WDSubscription;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.websocket.WDException;
import de.bund.zrb.util.PlaywrightEventMapper;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BrowserImpl implements Browser {
    private final List<WDScriptResult.AddPreloadScriptResult> globalScripts = new ArrayList<>();
    // ToDo: Make pages not static, to be able to handle multiple Browser instances:
    private static /*final*/ Pages pages;

    private final BrowserTypeImpl browserType;
    private final Process process;
    private final List<UserContextImpl> userContextImpls = new ArrayList<>();
    private final EventDispatcher dispatcher;
    private String defaultContextId = "default";

    private final WebDriver webDriver;

    public BrowserImpl(BrowserTypeImpl browserType, Process process, WebSocketImpl webSocketImpl) throws ExecutionException, InterruptedException {
        // ToDo: Make pages not static BUT FINAL, to be able to handle multiple Browser instances, see above:
        this.pages = new Pages(this); // aka. BrowsingContexts / Navigables in WebDriver BiDi

        this.browserType = browserType;
        this.process = process;

        // ToDo: May be moved to WD4J partly
        WebSocketManager webSocketManager = new WebSocketManagerImpl(webSocketImpl);
        dispatcher = new EventDispatcher(new PlaywrightEventMapper());
        this.webDriver = new WebDriver(webSocketManager, dispatcher).connect(browserType.name());

        onContextSwitch(pages::setActivePageId);
        fetchDefaultData();

        loadGlobalScripts(); // load JavaScript code relevant for the working Playwright API
    }

    public static PageImpl getPage(WDBrowsingContext context) {
        return pages.get(context.value());
    }

    private void loadGlobalScripts() {
//        // Channel für Callback anlegen
//        String channelId = UUID.randomUUID().toString(); // Zufällige ID für den Channel
//        WDChannelValue channel = new WDChannelValue(new WDChannelValue.ChannelProperties(new WDChannel(channelId)));
//        String callbackScript = ScriptHelper.loadScript("scripts/callback.js")
//                .replace("<CHANNEL_ID>", channelId);
//        // Callback-Script für die Kommunikation mit dem Playwright-Server (über Message Events)
//        globalScripts.add(webDriver.script().addPreloadScript(callbackScript, Collections.singletonList(channel)));


        // 🔹 1️⃣ Channel für das Fokus-Tracking anlegen
        String focusChannelId = "focus-events-channel";  // Feste ID für Fokus-Events
        WDChannelValue focusChannel = new WDChannelValue(new WDChannelValue.ChannelProperties(new WDChannel(focusChannelId)));

        // 🔹 2️⃣ Fokus-Tracking PreloadScript registrieren
        globalScripts.add(webDriver.script().addPreloadScript(
                ScriptHelper.loadScript("scripts/focusTracker.js"),
                Collections.singletonList(focusChannel)  // Channel mit übergeben
        ));


        // Alle weiteren globalen Scripts
        globalScripts.add(webDriver.script().addPreloadScript(ScriptHelper.loadScript("scripts/events.js")));
        globalScripts.add(webDriver.script().addPreloadScript(ScriptHelper.loadScript("scripts/callback.js")));
        globalScripts.add(webDriver.script().addPreloadScript(ScriptHelper.loadScript("scripts/debug.js"))); // ToDo: Remove
        globalScripts.add(webDriver.script().addPreloadScript(ScriptHelper.loadScript("scripts/dragAndDrop.js")));
    }

    public void removeGlobalScripts() {
        for (WDScriptResult.AddPreloadScriptResult result : globalScripts) {
            webDriver.script().removePreloadScript(result.getScript().value());
        }
        globalScripts.clear();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
            webDriver.browser().getUserContexts().getUserContexts().forEach(context -> {
                System.out.println("UserContext: " + context.getUserContext().value());
                UserContextImpl uc = new UserContextImpl(this, context.getUserContext());
//                fetchDefaultBrowsingContexts(uc.getPages(), context.getUserContext().value());
                userContextImpls.add(uc);
            });
        } catch (WDException ignored) {}
    }

    private void fetchDefaultBrowsingContexts(Pages currentPages, String userContextId) {
        // Get all browsing contexts (pages / tabs) already available
        try {
            // Check if a context is already available
            WDBrowsingContextResult.GetTreeResult tree = webDriver.browsingContext().getTree();
            tree.getContexts().forEach(context -> {
                System.out.println("BrowsingContext: " + context.getContext().value());
                currentPages.add(new PageImpl(this, null, context.getContext()));

                // NOT WORKING
//                // ToDo: Find a solution for all preloaded scripts, without duplicated code
//                // 🔹 Falls der Tab existierte, muss das Preload-Skript nachträglich gesetzt werden
//                // 🔹 1️⃣ Channel für das Fokus-Tracking anlegen
//                String focusChannelId = "focus-events-channel";  // Feste ID für Fokus-Events
//                WDChannelValue focusChannel = new WDChannelValue(new WDChannelValue.ChannelProperties(new WDChannel(focusChannelId)));
//                // 🔹 2️⃣ Fokus-Tracking PreloadScript registrieren
//                webDriver.script().addPreloadScript(
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
        // ToDo: Implement
    }

    @Override
    public void offDisconnected(Consumer<Browser> handler) {
        // ToDo: Implement
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
        return webDriver.isConnected();
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
        // ToDo: Hier muss ein neuer UserContext (aka. BrowserContext) erstellt werden, anstatt den Default UserContext zu verwenden
        PageImpl page = new PageImpl(this);
        page.onClose((e) -> {
            pages.remove(page.getBrowsingContextId());
        });
        pages.add(page);
        return page;
    }

    @Override
    public void startTracing(Page page, StartTracingOptions options) {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public byte[] stopTracing() {
        throw new UnsupportedOperationException("Not implemented!");
    }

    @Override
    public String version() {
        throw new UnsupportedOperationException("Not implemented!");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Compatibility with WebDriver BiDi
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// ToDo: Think about where to put the service instances
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public WebDriver getWebDriver() {
        return webDriver;
    }

    public WDScriptManager getScriptManager() {
        return webDriver.script();
    }

    public Pages getPages() {
        return pages;
    }

    public List<UserContextImpl> getUserContextImpls() {
        return userContextImpls;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    void onMessage(Consumer<WDScriptEvent.Message> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.MESSAGE.getName(), null, null);
            WDSubscription tmp = webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    private void offMessage(Consumer<WDScriptEvent.Message> handler) {
        // ToDo: Will not work without the browsingContextId, thus it has to use the SubscriptionId, in future!
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.MESSAGE.getName(), null, handler);
        }
    }

    public void onContextSwitch(Consumer<String> handler) {
        if (handler != null) {
            onMessage(message -> {
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public <T> WDSubscription addEventListener(WDSubscriptionRequest subscriptionRequest, Consumer<T> handler) {
        return dispatcher.addEventListener(subscriptionRequest, handler, webDriver.session());
    }

    public <T> void removeEventListener(String eventType, String browsingContextId, Consumer<T> listener) {
        dispatcher.removeEventListener(eventType, browsingContextId, listener, webDriver.session());
    }

    // ToDo: Not supported yet
    public <T> void removeEventListener(WDSubscription subscription, Consumer<T> listener) {
        dispatcher.removeEventListener(subscription, listener, webDriver.session());
    }

    @Deprecated // Since it does neither use the subscription id nor the browsing context id, thus terminating all listeners for the event type
    public <T> void removeEventListener(String eventType, Consumer<T> listener) {
        dispatcher.removeEventListener(eventType, listener, webDriver.session());
    }
}
