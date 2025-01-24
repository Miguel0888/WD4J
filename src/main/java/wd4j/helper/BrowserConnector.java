package wd4j.helper;

import wd4j.core.WebSocketConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Hilfsklasse für den Aufbau der WebSocket-Verbindung zum Browser, da der Verbindungsaufbau sich je nach Browser-Typ
 * unterscheidet.
 */
public class BrowserConnector {

//    public static String getWebSocketUrl(int port) throws Exception {
//        // URL für die Debugging-Schnittstelle
//        String endpointUrl = "http://localhost:" + port + "/json";
//
//        Thread.sleep(2000); // 2 Sekunden warten, bevor die Debugging-Schnittstelle abgefragt wird
//
//        // HTTP-Verbindung aufbauen
//        HttpURLConnection connection = (HttpURLConnection) new URL(endpointUrl).openConnection();
//        connection.setRequestMethod("GET");
//
//        // Antwort lesen
//        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
//            StringBuilder response = new StringBuilder();
//            String line;
//            while ((line = reader.readLine()) != null) {
//                response.append(line);
//            }
//
//            // JSON-Antwort zurückgeben
//            return parseWebSocketDebuggerUrl(response.toString());
//        }
//    }

    // ToDo: May fail if localhost:port/json is not reachable (WebDriver BiDi)
    public static String getWebSocketUrl(int port) throws Exception {
        String endpointUrl = "http://localhost:" + port + "/json";
        int retries = 10; // Anzahl der Wiederholungen
        int delayMs = 500; // Wartezeit zwischen den Versuchen

        for (int i = 0; i < retries; i++) {
            try {
                // HTTP-Verbindung aufbauen
                HttpURLConnection connection = (HttpURLConnection) new URL(endpointUrl).openConnection();
                connection.setRequestMethod("GET");

                // Antwort lesen
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // JSON-Antwort zurückgeben
                    return parseWebSocketDebuggerUrl(response.toString());
                }
            } catch (IOException e) {
                System.out.println("Versuch " + (i + 1) + " - Debugging-Endpunkt nicht erreichbar: " + e.getMessage());
                Thread.sleep(delayMs);
            }
        }

        throw new RuntimeException("Debugging-Endpunkt konnte nach mehreren Versuchen nicht erreicht werden.");
    }

    public static String getWebSocketUrlFromLog(Process browserProcess) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(browserProcess.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("[Firefox Log] " + line);
                if (line.contains("WebDriver BiDi listening on")) {
                    // Extrahiere die WebSocket-URL
                    return line.split("on ")[1].trim();
                }
            }
        }
        throw new RuntimeException("WebSocket URL konnte nicht aus dem Firefox-Log extrahiert werden.");
    }

    private static String parseWebSocketDebuggerUrl(String jsonResponse) {
        // Vereinfachtes Parsing für die WebSocket-URL
        // Suchen nach "ws://" in der JSON-Antwort
        int wsIndex = jsonResponse.indexOf("ws://");
        if (wsIndex == -1) {
            throw new IllegalStateException("Keine WebSocket-URL in der Antwort gefunden.");
        }

        // WebSocket-URL extrahieren
        int endIndex = jsonResponse.indexOf("\"", wsIndex);
        return jsonResponse.substring(wsIndex, endIndex);
    }

    public static WebSocketConnection getConnection(BrowserType browserType, String websocketUrl, int port) throws Exception {
        WebSocketConnection webSocketConnection;
        if(browserType == BrowserType.FIREFOX)
        {
            webSocketConnection = new WebSocketConnection(new URI(websocketUrl + "/session"));
        }
        else { // Chrome & Edge
            webSocketConnection = new WebSocketConnection(URI.create(getWebSocketUrl(port)));
        }
        return webSocketConnection;
    }
}
