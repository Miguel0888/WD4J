package wd4j.impl.playwright;

import wd4j.impl.manager.WDSessionManager;
import wd4j.impl.support.EventDispatcher;
import wd4j.impl.webdriver.command.response.WDSessionResult;
import wd4j.impl.webdriver.event.WDEventMapping;
import wd4j.impl.webdriver.event.WDScriptEvent;
import wd4j.impl.webdriver.type.session.WDSubscription;
import wd4j.impl.webdriver.type.session.WDSubscriptionRequest;
import wd4j.impl.websocket.WDException;
import wd4j.impl.websocket.WebSocketManager;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * TODO: DIE SESSION GIBT ES NICHT IN PLAYWRIGHT, DER CONTEXT MEINT DEN USER-CONTEXT UND IST OPTIONAL!
 *  Z.B. KANN MAN browser.newContext() AUFRUFEN, UM EINEN NEUEN KONTEXT ZU ERSTELLEN
 *  ODER MAN KANN DIREKT newPage() AUFRUFEN, UM EINE NEUE SEITE - OHNE USER-CONTEXT - ZU ERSTELLEN.
 *
 *  Differs from the W3C BrowsingContext Module in that it includes the Session Module. (The reason for this may be that
 *  the Chromium DevTools Protocol has a different understanding of what a Context and what a Session is.)
 *  See: https://playwright.dev/java/docs/api/class-browsercontext
 *
 *  Probably, the BrowserContext (PlayWright term) is the Session (corresponding W3C term)
 *  and the Page (PlayWright term) is the Context (corresponding W3C term).
 *
 *  => Better use the terms Session and Page to avoid confusion.
 */
public class Session {
    private String sessionId;
    private final WebSocketManager webSocketManager;
    private final WDSessionManager sessionManager;
    private final BrowserImpl browser;
    private final EventDispatcher dispatcher;

    /**
     * Creates a new session aka. browsing context.
     * @param browser The browser instance.
     * @throws WDException if the session cannot be created (mostly in case of an duplicated session)
     */
    public Session(BrowserImpl browser) throws WDException {
        this(browser, null);
    }

    /**
     * Creates a new session aka. browsing context.
     * @param browser The browser instance.
     * @param sessionId The ID of an existing session or null to create a new session.
     * @throws WDException if the session cannot be created (mostly in case of an duplicated session)
     */
    public Session(BrowserImpl browser, String sessionId) throws WDException {
        this.webSocketManager = browser.getWebSocketManager();
        this.browser = browser;
        this.dispatcher = webSocketManager.getEventDispatcher(); // ToDo: Remove this

        String browserName = browser.browserType().name();

        try {
            this.sessionManager = new WDSessionManager(webSocketManager);
            if (sessionId != null) {
                this.sessionId = sessionId;
            } else {
                createSession(browserName);
            }
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Start WebDriver BiDi's Event Extension to receive JavaScript Events
        // ToDo: DTO-Mapping
        onMessage(message -> {
            System.out.println("******************** Message: " + message.getType());
        });
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    private void onMessage(Consumer<WDScriptEvent.Message> handler) {
        if (handler != null) {
            WDSubscriptionRequest wdSubscriptionRequest = new WDSubscriptionRequest(WDEventMapping.MESSAGE.getName(), null, null);
            WDSubscription tmp = this.addEventListener(wdSubscriptionRequest, handler);
        }
    }

    private void offMessage(Consumer<WDScriptEvent.Message> handler) {
        // ToDo: Will not work without the browsingContextId, thus it has to use the SubscriptionId, in future!
        if (handler != null) {
            this.removeEventListener(WDEventMapping.MESSAGE.getName(), null, handler);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public BrowserImpl getBrowser() {
        return browser;
    }

    public WDSessionManager getSessionManager() {
        return sessionManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public <T> WDSubscription addEventListener(WDSubscriptionRequest subscriptionRequest, Consumer<T> handler) {
        return dispatcher.addEventListener(subscriptionRequest, handler, sessionManager);
    }

    public <T> void removeEventListener(String eventType, String browsingContextId, Consumer<T> listener) {
        dispatcher.removeEventListener(eventType, browsingContextId, listener, sessionManager);
    }

    // ToDo: Not supported yet
    public <T> void removeEventListener(WDSubscription subscription, Consumer<T> listener) {
        dispatcher.removeEventListener(subscription, listener, sessionManager);
    }

    @Deprecated // Since it does neither use the subscription id nor the browsing context id, thus terminating all listeners for the event type
    public <T> void removeEventListener(String eventType, Consumer<T> listener) {
        dispatcher.removeEventListener(eventType, listener, sessionManager);
    }

    /**
     * Creates a new session aka. browsing context.
     * The default contextId is not provided by every browser.
     * E.g. Firefox ESR does not provide a default contextId, whereas the normal Firefox does.
     *
     * To avoid this issue, you can also create a new context every time you launch a browser. Thus, this method is optional.
     */
    private void createSession(String browserName) throws InterruptedException, ExecutionException {
        // Create a new session
        WDSessionResult.NewResult sessionResponse = sessionManager.newSession(browserName); // ToDo: Does not work with Chrome!

        // Kontext-ID extrahieren oder neuen Kontext erstellen
        if (sessionResponse == null) {
            throw new IllegalArgumentException("SessionResponse darf nicht null sein!");
        }
        sessionId = sessionResponse.getSessionId();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}