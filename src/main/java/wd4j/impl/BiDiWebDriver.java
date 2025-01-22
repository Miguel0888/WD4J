package wd4j.impl;

import wd4j.api.By;
import wd4j.api.WebDriver;
import wd4j.api.WebElement;
import wd4j.impl.Command;
import java.net.URI;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class BiDiWebDriver implements WebDriver {
    private final WebSocketConnection webSocketConnection;
    private final BrowserType browserType;
    private final int port;

    public BiDiWebDriver(BrowserType browserType, int port) {
        this.browserType = browserType;
        this.port = port;

        try {
            // Browser starten
            Process browserProcess = browserType.launch(port);
            System.out.println(browserType.name() + " gestartet auf Port " + port);

            // WebSocket-Verbindung herstellen
            String websocketUrl = WebSocketEndpointHelper.getWebSocketUrl(port);
            this.webSocketConnection = new WebSocketConnection(new URI(websocketUrl));
            this.webSocketConnection.connect();
        } catch (Exception e) {
            throw new RuntimeException("Fehler beim Starten des Browsers oder Aufbau der WebSocket-Verbindung", e);
        }

        // Verbindung im Kontext setzen
        WebDriverContext.setConnection(webSocketConnection);
    }

    @Override
    public WebElement findElement(By locator) {
        String command = createFindElementCommand(locator);
        webSocketConnection.send(command);

        try {
            String response = webSocketConnection.receive();
            return new BiDiWebElement(response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Fehler beim Empfangen der Antwort", e);
        }
    }

    @Override
    public void get(String url) {
        Gson gson = new Gson();

        // Befehl für getTree erstellen
        Command getContextCommand = new Command(2, "browsingContext.getTree", new Object());
        String getContextCommandJson = gson.toJson(getContextCommand);
        webSocketConnection.send(getContextCommandJson);

        String response;
        try {
            response = webSocketConnection.receive();
            System.out.println("Received context response: " + response);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Failed to retrieve context", e);
        }

        // Extrahiere die Context-ID aus der Antwort
        String contextId = extractContextIdWithGson(response);

        // Befehl für navigate erstellen
        Command navigateCommand = new Command(1, "browsingContext.navigate",
            new JsonObjectBuilder()
                .addProperty("url", url)
                .addProperty("context", contextId)
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

    // Methode zum Schließen der Verbindung und Bereinigung des Kontextes
    public void close() {
        WebDriverContext.clearConnection();
    }

    private String createFindElementCommand(By locator) {
        String using = locator.getStrategy();
        String value = locator.getValue();

        return String.format(
                "{\"id\":4,\"method\":\"element.get\",\"params\":{\"using\":\"%s\",\"value\":\"%s\"}}",
                using, value
        );
    }

    private String extractContextIdWithGson(String response) {
        Gson gson = new Gson();
        try {
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            JsonObject result = jsonResponse.getAsJsonObject("result");
            if (result != null) {
                return result.getAsJsonArray("contexts").get(0).getAsJsonObject().get("context").getAsString();
            }
        } catch (JsonParseException | NullPointerException e) {
            throw new RuntimeException("Failed to parse context ID from response: " + response, e);
        }
        throw new RuntimeException("Context ID not found in response: " + response);
    }
    
}
