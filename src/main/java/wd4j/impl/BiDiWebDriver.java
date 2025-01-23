package wd4j.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.api.By;
import wd4j.api.WebDriver;
import wd4j.api.WebElement;
import wd4j.impl.*;
import wd4j.impl.modules.Session;

import java.net.URI;

public class BiDiWebDriver implements WebDriver {
    private final WebSocketConnection webSocketConnection;
    private final BrowserType browserType;
    private final int port;
    private final Session session;
    private String defaultContextId; // Speichert die Standard-Kontext-ID

    public BiDiWebDriver(BrowserType browserType) {
        this.browserType = browserType;
        this.port = browserType.getPort();

        try {
            // Browser starten
            Process browserProcess = browserType.launch();
            System.out.println(browserType.name() + " gestartet auf Port " + port);

            // WebSocket-Verbindung herstellen
            String websocketUrl = WebSocketEndpointHelper.getWebSocketUrl(port);
            this.webSocketConnection = new WebSocketConnection(new URI(websocketUrl));

            // Hinzufügen eines Listeners für die WebSocket-Verbindung
            this.webSocketConnection.setOnClose((code, reason) -> {
                System.out.println("CALLBACK: WebSocket closed. Code: " + code + ", Reason: " + reason);
                if (browserProcess.isAlive()) {
                    browserProcess.destroyForcibly();
                    System.out.println("Browser-Prozess wurde beendet.");
                }
            });

            this.webSocketConnection.connect();

            // Session initialisieren
            this.session = new Session(webSocketConnection);
            String sessionResponse = session.newSession(browserType.name()).get();
            System.out.println("Session gestartet: " + sessionResponse);

            // Standard-Kontext-ID extrahieren
            this.defaultContextId = extractDefaultContextId(sessionResponse);
            System.out.println("Default Context ID: " + defaultContextId);

        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Starten des Browsers oder Aufbau der WebSocket-Verbindung", e);
        }

        // Verbindung im Kontext setzen
        WebDriverContext.setConnection(webSocketConnection);
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
