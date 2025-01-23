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

        if (result != null && result.has("browsingContext")) {
            return result.getAsJsonObject("browsingContext").get("context").getAsString();
        }

        // Wenn nicht vorhanden, rufe getTree auf, um die Context-ID zu erhalten
        System.out.println("--- Keine Context-ID in Session-Antwort. Führe browsingContext.getTree aus. ---");

        return fetchDefaultContextFromTree();
    }

    private String fetchDefaultContextFromTree() {
        Gson gson = new Gson();
        Command getTreeCommand = new Command(
                webSocketConnection.getNextCommandId(),
                "browsingContext.getTree",
                new JsonObject() // Kein Parameter erforderlich
        );

        String commandJson = gson.toJson(getTreeCommand);
        webSocketConnection.send(commandJson);

        try {
            String response = webSocketConnection.receive();
            System.out.println("browsingContext.getTree response: " + response);

            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            JsonObject result = jsonResponse.getAsJsonObject("result");

            if (result != null && result.has("contexts")) {
                return result.getAsJsonArray("contexts")
                        .get(0)
                        .getAsJsonObject()
                        .get("context")
                        .getAsString();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to retrieve context tree", e);
        }

        throw new IllegalStateException("Default browsing context not found in tree.");
    }


    @Override
    public WebElement findElement(By locator) {
        return null;
    }

    @Override
    public void get(String url) {
        if (defaultContextId == null) {
            throw new IllegalStateException("No browsing context available.");
        }

        Gson gson = new Gson();
        Command navigateCommand = new Command(
                webSocketConnection.getNextCommandId(),
                "browsingContext.navigate",
                new JsonObjectBuilder()
                        .addProperty("url", url)
                        .addProperty("context", defaultContextId)
                        .build()
        );

        String navigateCommandJson = gson.toJson(navigateCommand);
        webSocketConnection.send(navigateCommandJson);

        try {
            String navigationResponse = webSocketConnection.receive();
            System.out.println("Navigation response: " + navigationResponse);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Navigation failed", e);
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
        WebDriverContext.clearConnection();
        session.endSession();
    }
}
