package wd4j.helper;

import com.microsoft.playwright.impl.BrowserTypeImpl;
import wd4j.core.WebSocketConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Hilfsklasse für den Aufbau der WebSocket-Verbindung zum Browser, da der Verbindungsaufbau sich je nach Browser-Typ
 * unterscheiden kann, solange Chrome und Edge BiDi nicht per Default aktiviert haben sondern weiter auf CDP setzen.
 *
 * In der Klasse gibt es daher auch die Möglichkeit zusammen mit der BrowserConnector-Klasse für Chrome und Edge
 * statt BiDi die CDP zu verwenden. Allerdins müsste dann für alle Commands ein Adapter geschrieben werden, der die
 * BiDi-Commands in CDP-Commands umwandelt. Das ist nicht Ziel dieser Implementierung und wird daher nicht weiter
 * verfolgt.
 *
 * Man beachte, dass bei BiDi direkt eine WebSocket-Verbindung aufgebaut zu ws://localhost:port/session, während bei
 * CDP eine HTTP-Verbindung aufgebaut wird zu http://localhost:port/json und die WebSocket-URL aus der Antwort extrahiert
 * wird, womit dann die WebSocketClient gefüttert wird.
 */
public class BrowserConnector {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Fields
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Felder für die Konfigurationsoptionen
    protected String browserPath; // Pfad ist Instanzvariable, da er sich je nach Browser-Typ unterscheidet
    protected int port = 9222; // Standard-Port für die Debugging-Schnittstelle
    protected String profilePath = null;
    protected boolean headless = false;
    protected boolean noRemote = false;
    protected boolean disableGpu = false;
    protected boolean startMaximized = false;
    protected boolean useCdp = true; // For Chrome and Edge only - u may use CDP instead of BiDi, not implemented yet!
    private String webSocketEndpoint;
    // Thread-sicherer Speicher für die WebSocket-URL aus dem Terminal-Output:
    final String[] devToolsUrl = {null};

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//    public static BrowserTypeImpl getBrowserType(String browserName, int port, String profilePath, boolean headless, boolean disableGpu, boolean noRemote) {
//        BrowserTypeImpl browserTypeImpl = BrowserTypeImpl.valueOf(browserName.toUpperCase());
//
//        //ToDo: Fix this somehow (maybe via createOptions class)
////        port = browserTypeImpl.getPort();
////        browserTypeImpl.setPort(port);
////        browserTypeImpl.setProfilePath(profilePath);
////        browserTypeImpl.setHeadless(headless);
////        browserTypeImpl.setDisableGpu(disableGpu);
////        browserTypeImpl.setNoRemote(noRemote);
//        return browserTypeImpl;
//    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Initialization
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Browser Start & Connection
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Erstellt eine WebSocket-Verbindung zum Browser.
     * Je nach Browser-Typ wird die WebSocket-URL unterschiedlich aufgebaut.
     * Für Firefox wird die URL direkt aufgebaut, während für Chrome und Edge die URL aus dem Terminal-Output
     * extrahiert wird. Alternativ kann die Url auch aus dem /json-Endpoint extrahiert werden, der mit HTTP aufgerufen
     * wird.
     *
     * @param browserTypeImpl
     * @param websocketUrl
     * @param port
     * @return
     * @throws Exception
     */
    // ToDo: Move to BrowserTypeImpl (connect Method) and support CDP (not only Firefox)
//    public static WebSocketConnection getConnection(BrowserTypeImpl browserTypeImpl, String websocketUrl, int port) throws Exception {
//        WebSocketConnection webSocketConnection;
//        if(browserTypeImpl.name().toLowerCase() == "firefox")
//        {
//            String webSocketUrl = websocketUrl + browserTypeImpl.getWebsocketEndpoint();
//            System.out.println("FIREFOX with WebSocket URL " + webSocketUrl);
//            webSocketConnection = new WebSocketConnection(new URI(webSocketUrl));
//        }
//        else { // Chrome & Edge
//            // ToDo: Move missing methodes vom BrowserType Git History (0efdad8f 30.01.25) to this class!
//            // BiDi URL may differ from CDP URL in the future:
//            if( !useCdp() ) { // BiDi
//                String webSocketUrl = getWebSocketUrl(port) + browserTypeImpl.getWebsocketEndpoint();
//                System.out.println("Using BiDi with WebSocket URL " + webSocketUrl);
//                webSocketConnection = new WebSocketConnection(URI.create(webSocketUrl));
//            } else { // CDP
//                if( getDevToolsUrl() != null ) { // Use Terminal Output primarily
//                    System.out.println("Using CDP for with Terminal URL " + getDevToolsUrl());
//                    webSocketConnection = new WebSocketConnection(URI.create(getDevToolsUrl()));
//                } else {
//                    String webSocketUrl = getWebSocketUrl(port);
//                    System.out.println("Using CDP with /json URL " + webSocketUrl);
//                    webSocketConnection = new WebSocketConnection(URI.create(webSocketUrl));
//                }
//            }
//        }
//        return webSocketConnection;
//    }

    /**
     * Get the WebSocket URL from the JSON response of the debugging endpoint (CDP only).
     *
     * @param port
     * @return
     * @throws Exception
     */
    // ToDo: May fail if localhost:port/json is not reachable (WebDriver BiDi)
    private static String getWebSocketUrl(int port) throws Exception {
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

    @Deprecated
    private static String getWebSocketUrlFromLog(Process browserProcess) throws Exception {
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

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Helper Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void terminateProcess(Process process) {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }

}
