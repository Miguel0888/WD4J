package wd4j.impl;

import app.Main;
import app.controller.CallbackWebSocketServer;
import com.microsoft.options.BindingCallback;
import com.microsoft.options.FunctionCallback;
import wd4j.helper.RecorderService;
import wd4j.impl.manager.*;
import wd4j.impl.playwright.WebSocketImpl;
import wd4j.impl.support.EventDispatcher;
import wd4j.impl.support.EventDispatcherImpl;
import wd4j.impl.support.EventMapper;
import wd4j.impl.webdriver.command.response.WDSessionResult;
import wd4j.impl.webdriver.type.session.WDSubscription;
import wd4j.impl.webdriver.type.session.WDSubscriptionRequest;
import wd4j.impl.websocket.WebSocketManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * This class is the entry point for the low-level WebDriver API. Aggregates all WebDriver Modules at one place.
 * @see wd4j.impl.manager.WDBrowserManager
 * @see wd4j.impl.manager.WDSessionManager
 * @see wd4j.impl.manager.WDBrowsingContextManager
 * @see wd4j.impl.manager.WDScriptManager
 * @see wd4j.impl.manager.WDInputManager
 * @see wd4j.impl.manager.WDStorageManager
 * @see wd4j.impl.manager.WDNetworkManager
 * @see wd4j.impl.manager.WDLogManager
 * @see wd4j.impl.manager.WDWebExtensionManager
 *
 * @link https://www.w3.org/TR/webdriver-bidi/#modules
 * @link https://de.wikipedia.org/wiki/Fassade_(Entwurfsmuster)
 */
public class WebDriver {

    private final WebSocketManager webSocketManager;

    private WDBrowserManager browser;
    private WDSessionManager session;
    private WDBrowsingContextManager browsingContext;
    private WDScriptManager script;
    private WDInputManager input;
    private WDStorageManager storage;
    private WDNetworkManager network;
    private WDLogManager log;
    private WDWebExtensionManager webExtension;

    private final EventDispatcher dispatcher;
    private String sessionId;

    // Additional features
    private final Map<String, FunctionCallback> exposedFunctions = new HashMap<>();
    private final Map<String, BindingCallback> exposedBindings = new HashMap<>();

    // ToDo: Use WebSocket Interface instead of WebSocketImpl, here !!!
    public WebDriver(WebSocketImpl connection, EventMapper eventMapper) throws ExecutionException, InterruptedException {
        this.dispatcher = new EventDispatcherImpl(eventMapper);
        this.webSocketManager = new WebSocketManager(connection, dispatcher);

        this.browser = new WDBrowserManager(webSocketManager);
        this.session = new WDSessionManager(webSocketManager);
        this.browsingContext = new WDBrowsingContextManager(webSocketManager);
        this.script = new WDScriptManager(webSocketManager);
        this.input = new WDInputManager(webSocketManager);
        this.storage = new WDStorageManager(webSocketManager);
        this.network = new WDNetworkManager(webSocketManager);
        this.log = new WDLogManager(webSocketManager);
        this.webExtension = new WDWebExtensionManager(webSocketManager);

        // ToDo: May be moved out to playwrigth or even the app / ui, since it is used for the recorder
        toggleCallbackServer(true); // ✅ Callback-Server aktivieren
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Getter
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // WebDriver BiDi Modules
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public WDBrowserManager browser() {
        return browser;
    }

    public WDSessionManager session() {
        return session;
    }

    public WDBrowsingContextManager browsingContext() {
        return browsingContext;
    }

    public WDScriptManager script() {
        return script;
    }

    public WDInputManager input() {
        return input;
    }

    public WDStorageManager storage() {
        return storage;
    }

    public WDNetworkManager network() {
        return network;
    }

    public WDLogManager log() {
        return log;
    }

    public WDWebExtensionManager webExtension() {
        return webExtension;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Event Handling
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//    public <T> void on(WDEventType<T> event, Consumer<T> handler) {
//        connection.on(event.getName(), handler);
//    }
//
//    public <T> void off(WDEventType<T> event, Consumer<T> handler) {
//        connection.off(event.getName(), handler);
//    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // private (required for WebDriver Class)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates a new session aka. browsing context.
     *
     * Starts a new session with the default browser. For one Browser only one session can be active at a time.
     * Therefore, this is recognized as a WebDriver core functionality.
     *
     * The default contextId is not provided by every browser.
     * E.g. Firefox ESR does not provide a default contextId, whereas the normal Firefox does.
     *
     * To avoid this issue, you can also create a new context every time you launch a browser. Thus, this method is optional.
     */
    public WebDriver connect(String browserName) throws InterruptedException, ExecutionException {
        // ToDo: Maybe send status command first to check if a session is already active

        // Create a new session
        WDSessionResult.NewResult sessionResponse = session().newSession(browserName); // ToDo: Does not work with Chrome!

        // Kontext-ID extrahieren oder neuen Kontext erstellen
        if (sessionResponse == null) {
            throw new IllegalArgumentException("SessionResponse darf nicht null sein!");
        }
        sessionId = sessionResponse.getSessionId();
        return this; // fluent API
    }


    /**
     * Reuses a session with the default browser identified by the given session ID.
     * For one Browser only one session can be active at a time.
     * Therefore, this is recognized as a WebDriver core functionality.
     *
     * @param sessionId
     * @return
     */
    // ToDo: Check if a session ID can be reused, or a default session ID is provided and can be reused
    public WebDriver reconnect(String sessionId) {
        // Reuse the session
        // ToDo: check status ?
        this.sessionId = sessionId;
        return this; // fluent API
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public <T> WDSubscription addEventListener(WDSubscriptionRequest subscriptionRequest, Consumer<T> handler) {
        return dispatcher.addEventListener(subscriptionRequest, handler, session());
    }

    public <T> void removeEventListener(String eventType, String browsingContextId, Consumer<T> listener) {
        dispatcher.removeEventListener(eventType, browsingContextId, listener, session());
    }

    // ToDo: Not supported yet
    public <T> void removeEventListener(WDSubscription subscription, Consumer<T> listener) {
        dispatcher.removeEventListener(subscription, listener, session());
    }

    @Deprecated // Since it does neither use the subscription id nor the browsing context id, thus terminating all listeners for the event type
    public <T> void removeEventListener(String eventType, Consumer<T> listener) {
        dispatcher.removeEventListener(eventType, listener, session());
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Optional (can be located elsewhere)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Deprecated // since JSON Data might be received via Message Events (see WebDriverBiDi ChannelValue)
    private CallbackWebSocketServer callbackWebSocketServer;

    @Deprecated // since script.ChannelValue might be used for Callbacks (will lead to Message Events)
    private void toggleCallbackServer(boolean activate) {
        if (activate) {
            callbackWebSocketServer = new CallbackWebSocketServer(8080, message -> {
                Main.getScriptTab().appendLog(message);  // UI-Log aktualisieren
                RecorderService.getInstance().recordAction(message); // Aktion im Recorder speichern
            });
            callbackWebSocketServer.start();
        } else {
            try {
                callbackWebSocketServer.stop();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean isConnected() {
        return webSocketManager.isConnected();
    }

    public void registerFunction(String name, FunctionCallback callback) {
        exposedFunctions.put(name, callback);
    }

    public void registerBinding(String name, BindingCallback callback) {
        exposedBindings.put(name, callback);
    }

    public FunctionCallback getFunction(String name) {
        return exposedFunctions.get(name);
    }

    public BindingCallback getBinding(String name) {
        return exposedBindings.get(name);
    }
}
