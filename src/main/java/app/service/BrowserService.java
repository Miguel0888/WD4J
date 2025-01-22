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

    private Process browserProcess;

    public void launchBrowser(String browserName, int port, String profilePath, boolean headless, boolean disableGpu, boolean noRemote) {
        try {
            BrowserType browserType = BrowserType.valueOf(browserName);
            browserType.setProfilePath(profilePath);
            browserType.setHeadless(headless);
            browserType.setDisableGpu(disableGpu);
            browserType.setNoRemote(noRemote);

            browserType.launch(port);

            System.out.println(browserName + " gestartet mit den folgenden Optionen:");
            System.out.println("Port: " + port);
            System.out.println("Profile Path: " + profilePath);
            System.out.println("Headless: " + headless);
            System.out.println("Disable GPU: " + disableGpu);
            System.out.println("No Remote: " + noRemote);
        } catch (Exception e) {
            System.out.println("Fehler beim Starten des Browsers:");
            e.printStackTrace();
        }
    }


    public void terminateBrowser() {
        if (browserProcess != null) {
            browserProcess.destroy(); // Beendet den Browserprozess
            browserProcess = null;
            System.out.println("Browser-Prozess wurde beendet.");
        } else {
            System.out.println("Kein Browser-Prozess aktiv.");
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
