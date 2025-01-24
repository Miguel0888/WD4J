package wd4j.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import wd4j.api.By;
import wd4j.api.WebDriver;
import wd4j.api.WebElement;
import wd4j.impl.modules.Session;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class BiDiWebDriver implements WebDriver {
    private WebSocketConnection webSocketConnection = null;
    private final Session session;
    private final BrowserType browserType;
    private final int port;
    private String defaultContextId; // Speichert die Standard-Kontext-ID

    public BiDiWebDriver(BrowserType browserType) {
        this.browserType = browserType;
        this.port = browserType.getPort();

        try {
            // Browser starten
            Process browserProcess = browserType.launch();
            System.out.println(browserType.name() + " gestartet auf Port " + port);

            // ToDo: How is the right way to extract the WebSocket URL?
            String serverUrl = "http://127.0.0.1:9222/session"; // ToDo: Check port!
            String websocketUrl = "ws://127.0.0.1:" + port;

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////

            this.webSocketConnection = new WebSocketConnection(new URI(websocketUrl + "/session"));

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
    
    private Session createSession(BrowserType browserType) throws InterruptedException, ExecutionException {
        final Session session;
        // Session initialisieren
        session = new Session(webSocketConnection);
        String sessionResponse = session.newSession(browserType.name()).get();
        System.out.println("Session gestartet: " + sessionResponse);

        // Standard-Kontext-ID extrahieren
        this.defaultContextId = extractDefaultContextId(sessionResponse);
        System.out.println("Default Context ID: " + defaultContextId);
        return session;
    }

    private String extractDefaultContextId(String sessionResponse) {
        Gson gson = new Gson();
        JsonObject jsonResponse = gson.fromJson(sessionResponse, JsonObject.class);
        JsonObject result = jsonResponse.getAsJsonObject("result");
    
        // Prüfe, ob ein Browsing-Kontext in der Antwort enthalten ist
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

    @Override
    public void get(String url) {
        // if (defaultContextId == null) {
        //     throw new IllegalStateException("No browsing context available.");
        // }

        // Gson gson = new Gson();
        // Command navigateCommand = new Command(
        //         webSocketConnection,
        //         "browsingContext.navigate",
        //         new JsonObjectBuilder()
        //                 .addProperty("url", url)
        //                 .addProperty("context", defaultContextId)
        //                 .build()
        // );

        // String navigateCommandJson = gson.toJson(navigateCommand);
        // webSocketConnection.send(navigateCommandJson);

        // try {
        //     String navigationResponse = webSocketConnection.receive();
        //     System.out.println("Navigation response: " + navigationResponse);
        // } catch (InterruptedException e) {
        //     Thread.currentThread().interrupt();
        //     throw new RuntimeException("Navigation failed", e);
        // }
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
        WebDriverContext.clearConnection();
        session.endSession();
    }
}
