package wd4j.impl.support;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.api.Page;
import wd4j.impl.playwright.BrowserImpl;
import wd4j.impl.playwright.WebSocketImpl;
import wd4j.impl.manager.BrowsingContextManager;
import wd4j.impl.manager.SessionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Represents a browsing session. A session is a collection of pages that are created in the same context.
 * Playwright only supports one session per browser instance. Therefore, a new session is created every time a new browser is launched.
 * The session is used to create new browsing contexts.
 *
 * Because of the real limitations of common browsers, Playwrigth does not use a seperate session class like this one.
 */
public class BrowserSession {
    private final BrowserImpl browser;
    private final SessionManager sessionManager;
    private String sessionId;
    private final List<Page> pages = new ArrayList<>(); // aka. contexts in WebDriver BiDi
    private String defaultContextId; // ToDo: If found, it should be used to create a new page with this id

    public BrowserSession(BrowserImpl browser, String browserName) throws ExecutionException, InterruptedException {
        this.browser = browser;

        // Create a new session
        WebSocketImpl webSocketImpl = browser.getWebSocketConnection();
        sessionManager = new SessionManager(webSocketImpl);

        try {
            fetchDefaultSession(browserName);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new browsing context from the default context. The default contextId is not provided by every browser.
     * E.g. Firefox ESR does not provide a default contextId, whereas the normal Firefox does.
     *
     * To avoid this issue, you can also create a new context every time you launch a browser. Thus, this method is optional.
     */
    private void fetchDefaultSession(String browserName) throws InterruptedException, ExecutionException {
        // Create a new session
        String sessionResponse = sessionManager.newSession(browserName); // ToDo: Does not work with Chrome!

        // Kontext-ID extrahieren oder neuen Kontext erstellen
        sessionId = processSessionResponse(sessionResponse);

        if(defaultContextId != null) {
            // ToDo: Create Page
            System.out.println("Context ID: " + defaultContextId);
        }
    }

    private String processSessionResponse(String sessionResponse) {
        String sessionId = null;

        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(sessionResponse, JsonObject.class);
        JsonObject result = jsonResponse.getAsJsonObject("result");

        // Prüfe, ob die Antwort eine Session-ID enthält
        if (result != null && result.has("sessionId")) {
            sessionId = result.get("sessionId").getAsString();
            System.out.println("--- Session-ID gefunden: " + sessionId);
        }

        // Prüfe, ob ein Default Browsing-Kontext in der Antwort enthalten ist
        if (result != null && result.has("contexts")) {
            JsonObject context = result.getAsJsonArray("contexts")
                    .get(0)
                    .getAsJsonObject();
            if (context.has("context")) {
                defaultContextId = context.get("context").getAsString();
                System.out.println("--- Browsing Context-ID gefunden: " + defaultContextId);
            }
        }
        return sessionId;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getSessionId() {
        return sessionId;
    }

    public String getDefaultContextId() {
        return defaultContextId;
    }

    public SessionManager getSessionService() {
        return sessionManager;
    }
}
