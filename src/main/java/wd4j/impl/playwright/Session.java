package wd4j.impl.playwright;

import wd4j.api.*;
import wd4j.impl.manager.WDSessionManager;
import wd4j.impl.support.EventDispatcher;
import wd4j.impl.webdriver.command.response.WDSessionResult;
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
    //ToDo: Move support.BrowserSession Class in here..
    private String sessionId;
    private final WebSocketManager webSocketManager;
    private final WDSessionManager WDSessionManager;
    private final BrowserImpl browser;
    private final EventDispatcher dispatcher;


    public Session(WebSocketManager webSocketManager, BrowserImpl browser) {
        this(webSocketManager, browser, null);
    }

    public Session(WebSocketManager webSocketManager, BrowserImpl browser, Browser.NewContextOptions options) {
        this.webSocketManager = webSocketManager;
        this.browser = browser;
        this.dispatcher = webSocketManager.getEventDispatcher();

        String browserName = browser.browserType().name();

        if (options != null) {
            // ToDo: Use options to create a new session
        }

        try {
            this.WDSessionManager = new WDSessionManager(webSocketManager);
            fetchDefaultSession(browserName);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public BrowserImpl getBrowser() {
        return browser;
    }

    public WDSessionManager getSessionManager() {
        return WDSessionManager;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public <T> void addEventListener(String eventName, Consumer<T> handler, Class<T> eventClass) {
        dispatcher.addEventListener(eventName, handler, eventClass, WDSessionManager);
    }

    public <T> void removeEventListener(String eventType, Consumer<T> listener) {
        dispatcher.removeEventListener(eventType, listener, WDSessionManager);
    }

    /**
     * Creates a new session aka. browsing context.
     * The default contextId is not provided by every browser.
     * E.g. Firefox ESR does not provide a default contextId, whereas the normal Firefox does.
     *
     * To avoid this issue, you can also create a new context every time you launch a browser. Thus, this method is optional.
     */
    private void fetchDefaultSession(String browserName) throws InterruptedException, ExecutionException {
        // Create a new session
        WDSessionResult.NewSessionResult sessionResponse = WDSessionManager.newSession(browserName); // ToDo: Does not work with Chrome!

        // Kontext-ID extrahieren oder neuen Kontext erstellen
        sessionId = processSessionResponse(sessionResponse);
    }

    private String processSessionResponse(WDSessionResult.NewSessionResult sessionResponse) {
        if (sessionResponse == null) {
            throw new IllegalArgumentException("SessionResponse darf nicht null sein!");
        }

        // 1️⃣ **Session-ID extrahieren**
        String sessionId = sessionResponse.getSessionId();
        if (sessionId != null) {
            System.out.println("--- Session-ID gefunden: " + sessionId);
        } else {
            System.out.println("⚠ Keine Session-ID gefunden!");
        }

        // 2️⃣ **Default Browsing-Kontext prüfen (Optional)**
        // ⚠ Derzeit ist in der Spezifikation KEIN "contexts"-Feld vorgesehen. Muss ggf. zum DTO hinzugefügt werden!

        return sessionId;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
}