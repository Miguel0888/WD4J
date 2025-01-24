package wd4j.core;

import wd4j.helper.BrowserConnector;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.api.By;
import wd4j.api.WebDriver;
import wd4j.api.WebElement;
import wd4j.helper.BrowserType;
import wd4j.helper.JsonObjectBuilder;
import wd4j.impl.modules.BrowsingContext;
import wd4j.impl.modules.Session;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class BiDiWebDriver implements WebDriver {
    private final Process browserProcess; // The Handle to the Browser Process
    private final String websocketUrl;
    private WebSocketConnection webSocketConnection = null;
    private final Session session;
    private final BrowserType browserType;
    private final int port;
    private String contextId; // Speichert die Standard-Kontext-ID
    private List<BrowsingContext> browsingContext = new ArrayList<>(); // ToDo: Maybe use a HashMap instead?

    public BiDiWebDriver(BrowserType browserType) {
        this.browserType = browserType;
        this.port = browserType.getPort(); // Makes sure the port is not changed!
        this.websocketUrl = "ws://127.0.0.1:" + port;

        try {
            // Browser starten
            browserProcess = browserType.launch();
            System.out.println(browserType.name() + " gestartet auf Port " + port);

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////

            // WebSocket-Verbindung erstellen
            webSocketConnection = BrowserConnector.getConnection(browserType, websocketUrl, port);

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////

            this.webSocketConnection.connect();
            System.out.println("WebSocket-Verbindung hergestellt: " + websocketUrl);

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////

            session = createSession(browserType);
            System.out.println("Session erstellt: " + session);

        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Starten des Browsers oder Aufbau der WebSocket-Verbindung", e);
        }

        // Verbindung im Kontext setzen // ToDo
//        WebDriverContext.setConnection(webSocketConnection);
    }

    private static String readInputStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /*
     * Required for Firefox ESR ?
     */
    // Hilfsmethode: Neuen Context erstellen
    private String createContext(String sessionId) {
        Command createContextCommand = new Command(
            webSocketConnection,
            "browsingContext.create",
            new JsonObjectBuilder()
                .addProperty("type", "tab") // Standardmäßig einen neuen Tab erstellen
                .build()
        );
    
        try {
            String response = webSocketConnection.send(createContextCommand);
            System.out.println("browsingContext.create response: " + response);
    
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            JsonObject result = jsonResponse.getAsJsonObject("result");
    
            if (result != null && result.has("context")) {
                String contextId = result.get("context").getAsString();
                System.out.println("--- Neuer Context erstellt: " + contextId);
                return contextId;
            }
        } catch (RuntimeException e) {
            System.out.println("Error creating context: " + e.getMessage());
            throw e;
        }
    
        throw new IllegalStateException("Failed to create context using sessionId: " + sessionId);
    }
    
    /**
     * Erstellt eine neue Session und gibt diese zurück.
     * Da einige Browser einen Standard-Kontext erstellen, wird mti diesem direkt ein neuer Browsing-Kontext erstellt.
     * Damit das Verhalten konsistent ist, wird ein neuer Kontext erstellt, wenn kein Standard-Kontext gefunden wird.
     *
     * @param browserType Der Browsertyp
     * @return Die erstellte Session
     */
    private Session createSession(BrowserType browserType) throws InterruptedException, ExecutionException {
        final Session session;
        // Session initialisieren
        session = new Session(webSocketConnection);
        String sessionResponse = session.newSession(browserType.name()).get();

        // Debug output
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("Session response: " + sessionResponse);
        System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

        // Kontext-ID extrahieren oder neuen Kontext erstellen
        this.contextId = extractDefaultContextIdOrCreateNewContextOtherwise(sessionResponse);
        System.out.println("Context ID: " + contextId);

        // BrowsingContext erstellen und speichern
        // ToDo: Might be the wrong place, but works for now!
        this.browsingContext.add(new BrowsingContext(webSocketConnection, contextId));
        System.out.println("BrowsingContext erstellt mit ID: " + contextId);

        return session;
    }

    private String extractDefaultContextIdOrCreateNewContextOtherwise(String sessionResponse) {
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(sessionResponse, JsonObject.class);
        JsonObject result = jsonResponse.getAsJsonObject("result");
    
        // Prüfe, ob ein Default Browsing-Kontext in der Antwort enthalten ist
        if (result != null && result.has("contexts")) {
            JsonObject context = result.getAsJsonArray("contexts")
                                        .get(0)
                                        .getAsJsonObject();
            if (context.has("context")) {
                String contextId = context.get("context").getAsString();
                System.out.println("--- Browsing Context-ID gefunden: " + contextId);
                return contextId;
            }
        }
    
        // Prüfe, ob die Antwort eine Session-ID enthält
        if (result != null && result.has("sessionId")) {
            String sessionId = result.get("sessionId").getAsString();
            System.out.println("--- Session-ID gefunden: " + sessionId);
    
            // Fallback: Browsing-Kontext abrufen, wenn nötig
            System.out.println("--- Keine Context-ID in Session-Antwort. Führe browsingContext.create aus. ---");
            return createContext(sessionId);
        }
    
        // Fallback zu browsingContext.getTree, wenn kein Kontext gefunden wurde
        System.out.println("--- Weder Context-ID noch Session-ID gefunden. Führe browsingContext.getTree aus. ---");
        return fetchDefaultContextFromTree();
    }
    

    // Fallback-Methode: Kontext über getTree suchen
    private String fetchDefaultContextFromTree() {
        Command getTreeCommand = new Command(
            webSocketConnection,
            "browsingContext.getTree",
            new JsonObject() // Kein Parameter erforderlich
        );
    
        try {
            String response = webSocketConnection.send(getTreeCommand);
            System.out.println("browsingContext.getTree response: " + response);
    
            JsonObject jsonResponse = new Gson().fromJson(response, JsonObject.class);
            JsonObject result = jsonResponse.getAsJsonObject("result");
    
            if (result != null && result.has("contexts")) {
                return result.getAsJsonArray("contexts")
                        .get(0)
                        .getAsJsonObject()
                        .get("context")
                        .getAsString();
            }
        } catch (RuntimeException e) {
            System.out.println("Error fetching context tree: " + e.getMessage());
            throw e;
        }
    
        throw new IllegalStateException("Default browsing context not found in tree.");
    }
    

    @Override
    public WebElement findElement(By locator) {
        return null;
    }

    // ToDo: Check if "get" is standard conform
    @Override
    public void get(String url) {
        try
        {
            browsingContext.get(0).navigate(url); // ToDo: Check if index 0 is always the right one!
        }
        catch( ExecutionException | InterruptedException e)
        {
            // Todo
        }
    }

    @Override
    public String getCurrentUrl() {
        return "";
    }

    @Override
    public String getTitle() {
        return "";
    }

    @Override
    public void close() {
        // ToDo: Maybe close all BrowsingContexts?
        session.endSession();
    }

    /**
     * Returns the BrowsingContext at the given index.
     * A BrowsingContext is a tab or window in the browser!
     *
     * @param index The index of the BrowsingContext.
     * @return The BrowsingContext at the given index.
     */
    // ToDo: Not very good practice?!?
    public BrowsingContext getBrowsingContext(int index) {
        return browsingContext.get(index);
    }
}
