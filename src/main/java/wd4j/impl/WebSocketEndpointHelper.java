package wd4j.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class WebSocketEndpointHelper {

    public static String getWebSocketUrl(int port) throws Exception {
        // URL für die Debugging-Schnittstelle
        String endpointUrl = "http://localhost:" + port + "/json";

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
}
