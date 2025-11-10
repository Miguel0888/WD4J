package de.bund.zrb;

import com.microsoft.playwright.*;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import de.bund.zrb.manager.WDInputManager;
import de.bund.zrb.manager.WDScriptManager;
import de.bund.zrb.api.WDWebSocketManager;
import de.bund.zrb.support.ScriptHelper;
import de.bund.zrb.command.response.WDBrowsingContextResult;
import de.bund.zrb.command.response.WDScriptResult;
import de.bund.zrb.type.script.*;
import de.bund.zrb.websocket.WDErrorResponse;
import de.bund.zrb.websocket.WDEventNames;
import de.bund.zrb.event.WDScriptEvent;
import de.bund.zrb.type.browsingContext.WDBrowsingContext;
import de.bund.zrb.type.session.WDSubscription;
import de.bund.zrb.type.session.WDSubscriptionRequest;
import de.bund.zrb.win.Win32Windows;

import javax.swing.*;
import java.beans.PropertyChangeSupport;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class BrowserImpl implements Browser {
    public static final String CHANNEL_FOCUS_EVENTS = "focus-events-channel";
    public static final String CHANNEL_RECORDING_EVENTS = "recording-events-channel";
    public static final String DEFAULT_USER_CONTEXT = "default";

    private final RecordingEventRouter router;

    private final List<WDScriptResult.AddPreloadScriptResult> globalScripts = new ArrayList<>();

    private final BrowserTypeImpl browserType;
    private final Process process;
    private final List<UserContextImpl> userContextImpls = new ArrayList<>();
    private String defaultContextId = "default";

    private final WebDriver webDriver;

    private String activePageId;
    private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    // Top-Level Fensterhandle (Windows)
    private volatile WinDef.HWND topLevelHwnd;

    private final List<Consumer<Browser>> disconnectListeners = new ArrayList<Consumer<Browser>>();

    public BrowserImpl(BrowserTypeImpl browserType, Process process, WDWebSocketImpl webSocketImpl) throws ExecutionException, InterruptedException {
        router = new RecordingEventRouter(this); // ToDo

        this.browserType = browserType;
        this.process = process;

        // ToDo: May be moved to WD4J partly
        WDWebSocketManager webSocketManager = new WDWebSocketManagerImpl(webSocketImpl);
        this.webDriver = new WebDriver(webSocketManager).connect(browserType.name());

        onContextSwitch(this::setActivePageId);
        onRecordingEvent(BrowserImpl.CHANNEL_RECORDING_EVENTS, this::handleRecordingEvent);
        fetchDefaultData();

        loadGlobalScripts(); // load JavaScript code relevant for the working Playwright API

        // Prozess-Beobachtung: Wenn der externe Browser-Prozess endet, melden und räumen wir auf
        new Thread(() -> {
            try {
                process.waitFor();
                for (Consumer<Browser> l : new ArrayList<Consumer<Browser>>(disconnectListeners)) {
                    try { l.accept(this); } catch (Throwable ignore) {}
                }
            } catch (InterruptedException ignored) {
            }
        }, "browser-proc-watcher").start();
    }

    /** Liefert das HWND des Top-Level Browserfensters (cached). */
    public WinDef.HWND getTopLevelHwnd() {
        return resolveTopLevelHwnd();
    }

    /** Ermittelt (und cached) das HWND des gestarteten Browser-Prozesses. */
    private WinDef.HWND resolveTopLevelHwnd() {
        WinDef.HWND cached = this.topLevelHwnd;
        if (cached != null) return cached;
        try {
            int pid = getWindowsPid(this.process);
            if (pid <= 0) return null;
            WinDef.HWND hwnd = Win32Windows.waitForTopLevelWindowOfPid(pid, Duration.ofSeconds(10));
            this.topLevelHwnd = hwnd;
            return hwnd;
        } catch (Throwable t) {
            System.err.println("[Video] HWND-Auflösung fehlgeschlagen: " + t.getMessage());
            return null;
        }
    }

    /** Holt den Windows-PID des Java Process (gleich wie ehemals in BrowserTypeImpl). */
    private static int getWindowsPid(Process proc) {
        try {
            java.lang.reflect.Field f = proc.getClass().getDeclaredField("handle");
            f.setAccessible(true);
            long handleVal = f.getLong(proc);
            WinNT.HANDLE hProc = new WinNT.HANDLE();
            hProc.setPointer(Pointer.createConstant(handleVal));
            int pid = Kernel32.INSTANCE.GetProcessId(hProc);
            return pid; // 0 => Fehler
        } catch (Throwable t) {
            System.err.println("[PID] Konnte Windows-PID nicht ermitteln: " + t.getMessage());
            return 0;
        }
    }

    private void handleRecordingEvent(WDScriptEvent.MessageWD message) {
        router.dispatch(message);
    }

    public RecordingEventRouter getRecordingEventRouter() {
        return router;
    }

    public PageImpl getPage(WDBrowsingContext context) {
        List<Page> allPages = getAllPages();
        if( context == null || context.value() == null) return null;
        return (PageImpl) allPages.stream().filter(p -> context.value().equals(((PageImpl) p).getBrowsingContextId())).findFirst().orElse(null);
    }

    private void loadGlobalScripts() {
        try
        {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ///  Events
            globalScripts.add(webDriver.script().addPreloadScript(
                    ScriptHelper.loadScript("scripts/focusTracker.js"),
                    Collections.singletonList(new WDChannelValue(new WDChannelValue.ChannelProperties(new WDChannel(CHANNEL_FOCUS_EVENTS))))  // Channel mit übergeben
            ));

            // Recorder Callback analog zum Fokus-Tracker:
            globalScripts.add(webDriver.script().addPreloadScript(
                    ScriptHelper.loadScript("scripts/recorder.js"),
                    Collections.singletonList(new WDChannelValue(new WDChannelValue.ChannelProperties(new WDChannel(CHANNEL_RECORDING_EVENTS))))
            ));

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ///  Alle weiteren globalen Scripts
            globalScripts.add(webDriver.script().addPreloadScript(ScriptHelper.loadScript("scripts/debug.js"))); // ToDo: Remove
            globalScripts.add(webDriver.script().addPreloadScript(ScriptHelper.loadScript("scripts/selector-overlay.js")));
            globalScripts.add(webDriver.script().addPreloadScript(ScriptHelper.loadScript("scripts/dragAndDrop.js")));
        } catch (NullPointerException ignore) {
            JOptionPane.showMessageDialog(null,
                    "Ein erforderliches JavaScript konnte nicht geladen werden. Es kann zu unerwarteten Verhalten kommen.",
                    "Fehler beim Laden der Resourcen", JOptionPane.WARNING_MESSAGE);
        }

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
        fetchDefaultBrowsingContexts();
        System.out.println("-----------------------------------");
        // ToDo: Fetch all Pages from every UserContext except the default one
    }

    private void fetchDefaultSessionData() {
        // Get all user contexts already available
        try {
            webDriver.browser().getUserContexts().getUserContexts().forEach(context -> {
                System.out.println("UserContext: " + context.getUserContext().value());
                UserContextImpl uc = new UserContextImpl(this, context.getUserContext());
                userContextImpls.add(uc);
            });
        } catch (WDErrorResponse ignored) {}
    }

    private void fetchDefaultBrowsingContexts() {
        try {
            WDBrowsingContextResult.GetTreeResult tree = webDriver.browsingContext().getTree();

            tree.getContexts().forEach(info -> {
                String contextId = info.getContext().value();
                String userContextId = info.getUserContext().value();

                System.out.println("BrowsingContext: " + contextId + ", UserContext: " + userContextId);

                // 🔑 Den passenden UserContextImpl finden:
                Optional<UserContextImpl> userContextOpt = userContextImpls.stream()
                        .filter(uc -> uc.getUserContext().value().equals(userContextId))
                        .findFirst();

                if (!userContextOpt.isPresent()) {
                    System.err.println("⚠️ Kein UserContextImpl für: " + userContextId);
                    return;
                }

                UserContextImpl userContext = userContextOpt.get();

                // 🔑 PageImpl bauen:
                PageImpl page = new PageImpl(this, userContext.getUserContext(), info.getContext());

                // 🔑 In den UserContext eintragen:
                userContext.register(page);

                // 🔑 Direkt prüfen, ob der Kontext aktiv ist:
                if (isContextFocused(info.getContext())) {
                    this.activePageId = contextId;
                    System.out.println("Initial activePageId: " + activePageId);
                }
            });

        } catch (WDErrorResponse ex) {
            ex.printStackTrace();
        }
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
        if (handler != null) disconnectListeners.add(handler);
    }

    @Override
    public void offDisconnected(Consumer<Browser> handler) {
        if (handler != null) disconnectListeners.remove(handler);
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
        for (Consumer<Browser> l : new ArrayList<Consumer<Browser>>(disconnectListeners)) {
            try { l.accept(this); } catch (Throwable ignore) {}
        }
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
        Optional<UserContextImpl> defaultContextOpt = userContextImpls.stream()
                .filter(uc -> uc.getUserContext().value().equals(DEFAULT_USER_CONTEXT))
                .findFirst();

        if (!defaultContextOpt.isPresent()) {
            throw new IllegalStateException("Kein default UserContext vorhanden!");
        }

        UserContextImpl defaultContext = defaultContextOpt.get();

        PageImpl page = (PageImpl) defaultContext.newPage();

        page.onClose((e) -> {
            defaultContext.pages().remove(page);
            System.out.println("🔒 Page entfernt: " + page.getBrowsingContextId());
        });

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

    public WDInputManager getInputManager() {
        return webDriver.input();
    }

    public List<UserContextImpl> getUserContextImpls() {
        return userContextImpls;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void onMessage(Consumer<WDScriptEvent.MessageWD> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventNames.MESSAGE.getName(), null, null);
            WDSubscription tmp = webDriver.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    private void offMessage(Consumer<WDScriptEvent.MessageWD> handler) {
        // ToDo: Will not work without the browsingContextId, thus it has to use the SubscriptionId, in future!
        if (handler != null) {
            webDriver.removeEventListener(WDEventNames.MESSAGE.getName(), null, handler);
        }
    }

    public void onContextSwitch(Consumer<String> handler) {
        if (handler != null) {
            onMessage(message -> {
                if (CHANNEL_FOCUS_EVENTS.equals(message.getParams().getChannel().value())) {
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

    public void onRecordingEvent(String channelName, Consumer<WDScriptEvent.MessageWD> handler) {
        if (handler != null) {
            onMessage(message -> {
                if (channelName.equals(message.getParams().getChannel().value())) {
                    // Kein Filter mehr!
                    handler.accept(message);
                }
            });
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setActivePageId(String s) {
        setActivePageId(s, false);
    }

    public void setActivePageId(String contextId, boolean isUiInitiated) {
        if(contextId == null)
        {
            return;
        }
        this.activePageId = contextId;
    }

    public String getActivePageId() {
        return activePageId;
    }

    public PageImpl getActivePage() {
        List<Page> allPages = getAllPages();
        if( activePageId == null) return null;
        return (PageImpl) allPages.stream().filter(p -> activePageId.equals(((PageImpl) p).getBrowsingContextId())).findFirst().orElse(null);
    }

    List<Page> getAllPages() {
        List<Page> all = new ArrayList<>();
        for (UserContextImpl ctx : userContextImpls) {
            all.addAll(ctx.pages());
        }
        return all;
    }

    public boolean isContextFocused(WDBrowsingContext ctx) {
        WDEvaluateResult eval = webDriver.script().evaluate("document.hasFocus();",
                new WDTarget.ContextTarget(ctx),
                true
        );
        if (eval instanceof WDEvaluateResult.WDEvaluateResultSuccess) {
            WDRemoteValue value = ((WDEvaluateResult.WDEvaluateResultSuccess) eval).getResult();
            if (value instanceof WDPrimitiveProtocolValue.BooleanValue) {
                return (( WDPrimitiveProtocolValue.BooleanValue) value).getValue();
            }
        }
        return false;
    }

}
