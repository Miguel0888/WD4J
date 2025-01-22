package app.service;

import wd4j.impl.BiDiWebSocketClient;
import wd4j.impl.BrowserType;
import wd4j.impl.WebSocketEndpointHelper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

public class BrowserService {
    int port = 9222;

    // Pfad zur Firefox-Binary (passe diesen an dein System an)
    String firefoxPath = "C:\\Program Files\\Mozilla Firefox\\firefox.exe";
    // Profilverzeichnis (optional, für benutzerdefinierte Einstellungen)
    String firefoxProfileDirectory = "C:\\FirefoxProfile";

    private String chromePath = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";
    private String chromeProfileDirectory = "C:\\ChromeProfile";

    private BiDiWebSocketClient client;

    public void launchBrowser(BrowserType browserType) {
        try {
            browserType.launch(port);

            Thread.sleep(1000); // Wartezeit, bis der Browser gestartet ist

            String websocketUrl = WebSocketEndpointHelper.getWebSocketUrl(port);
            System.out.println("WebSocket-URL: " + websocketUrl);

            client = new BiDiWebSocketClient(new URI(websocketUrl));
            client.connect();

            System.out.println("Browser gestartet.");
        } catch (Exception e) {
            System.out.println("Fehler beim Starten des Browsers:");
            e.printStackTrace();
        }
    }

    public void navigateTo(String url) {
        try {
            if (client != null) {
                // BiDi-Kommando zum Navigieren senden
                String command = String.format(
                        "{\"id\":1,\"method\":\"browsingContext.navigate\",\"params\":{\"url\":\"%s\"}}",
                        url
                );
                client.send(command);

                // Antwort empfangen und verarbeiten
                String response = client.receive();
                System.out.println("Navigationsantwort: " + response);
            } else {
                System.out.println("WebSocket-Client ist nicht verbunden. Bitte Browser starten.");
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Navigieren zu URL:");
            e.printStackTrace();
        }
    }


    public void closeBrowser() {
        try {
            if (client != null) {
                // BiDi-Kommando zum Schließen des Browsers senden
                String command = "{\"id\":2,\"method\":\"browsingContext.close\",\"params\":{}}";
                client.send(command);

                // Antwort empfangen und verarbeiten
                String response = client.receive();
                System.out.println("Antwort auf Schließen: " + response);

                // Verbindung schließen
                client.close();
            } else {
                System.out.println("WebSocket-Client ist nicht verbunden. Browser war möglicherweise bereits geschlossen.");
            }
        } catch (Exception e) {
            System.out.println("Fehler beim Schließen des Browsers:");
            e.printStackTrace();
        }
    }

}
