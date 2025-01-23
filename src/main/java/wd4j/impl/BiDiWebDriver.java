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
//            String serverUrl = "ws://127.0.0.1:" + port;
            String serverUrl = "http://127.0.0.1:9222/session"; // ToDo: Check port!
            String websocketUrl = "ws://127.0.0.1:" + port;

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////

//            // Session erstellen und WebSocket-URL extrahieren
//            String jsonResponse = connect(serverUrl);
//            System.out.println("WebSocket-URL: " + websocketUrl);
//            websocketUrl = extractWebSocketUrl(jsonResponse);


            this.webSocketConnection = new WebSocketConnection(new URI(websocketUrl));

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////
            this.webSocketConnection.connect();
            System.out.println("WebSocket-Verbindung hergestellt: " + websocketUrl);
            session = createSession(browserType);
            System.out.println("Session erstellt: " + session);

        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Starten des Browsers oder Aufbau der WebSocket-Verbindung", e);
        }

        // Verbindung im Kontext setzen // ToDo
//        WebDriverContext.setConnection(webSocketConnection);
    }

    public static String extractWebSocketUrl(String jsonResponse) {
        JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();
        return response.getAsJsonObject("value")
                .getAsJsonObject("capabilities")
                .get("webSocketUrl")
                .getAsString();
    }

    public static String connect(String serverUrl) throws IOException {
        // Erstelle den HTTP-Client
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // Baue die URL
            String endpoint = serverUrl + "/session";

            // Erstelle die POST-Anfrage
            HttpPost postRequest = new HttpPost(endpoint);

            // Setze die Header
            postRequest.addHeader("Content-Type", "application/json");

            // Baue die JSON-Payload
            String payload = buildInitPayload();
            postRequest.setEntity(new StringEntity(payload, ContentType.APPLICATION_JSON));

            // Führe die Anfrage aus und verarbeite die Antwort
            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                int statusCode = response.getCode();

                if (statusCode == 200) {
                    // Lese den Response-Body
                    return readInputStream(response.getEntity().getContent());
                } else {
                    throw new IOException("Failed to create session: HTTP " + statusCode);
                }
            }
        }
    }

    private static String readInputStream(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }


    public static String buildInitPayload() {
        // Hauptobjekt erstellen
        JsonObject payload = new JsonObject();

        // capabilities hinzufügen
        JsonObject capabilities = new JsonObject();
        JsonObject alwaysMatch = new JsonObject();
        alwaysMatch.addProperty("webSocketUrl", true);
        capabilities.add("alwaysMatch", alwaysMatch);

        payload.add("capabilities", capabilities);

        // Gson generiert daraus den JSON-String
        return new Gson().toJson(payload);
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
        return jsonResponse.getAsJsonObject("result").getAsJsonObject("capabilities")
                .getAsJsonObject("browsingContext").get("context").getAsString();
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
