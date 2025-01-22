package wd4j.impl;

import wd4j.api.By;
import wd4j.api.WebDriver;
import wd4j.api.WebElement;

import java.net.URI;

public class BiDiWebDriver implements WebDriver {
    private final WebSocketConnection webSocketConnection;

    public BiDiWebDriver(URI websocketUri) {
        this.webSocketConnection = new WebSocketConnection(websocketUri);
        try {
            this.webSocketConnection.connect();
            // Setze die Verbindung in den WebDriverContext
            WebDriverContext.setConnection(webSocketConnection);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Fehler beim Aufbau der WebSocket-Verbindung", e);
        }
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
        String command = String.format(
                "{\"id\":1,\"method\":\"browsingContext.navigate\",\"params\":{\"url\":\"%s\"}}",
                url
        );
        webSocketConnection.send(command);
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
}
