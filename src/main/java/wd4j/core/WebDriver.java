package wd4j.core;

import wd4j.helper.BrowserConnector;
import wd4j.helper.BrowserType;
import wd4j.impl.modules.Session;

import java.io.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class WebDriver {
    private final Process browserProcess; // The Handle to the Browser Process
    private final String websocketUrl;
    private WebSocketConnection webSocketConnection = null;
    private final Session session;
    private final BrowserType browserType;
    private final int port;

    public WebDriver(BrowserType browserType) {
        this.browserType = browserType;
        this.port = browserType.getPort(); // Makes sure the port is not changed!
        this.websocketUrl = "ws://127.0.0.1:" + port;

        try {
            // Browser starten
            browserProcess = browserType.launch();
            System.out.println(browserType.name() + " gestartet auf Port " + port);

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////

            // WebSocket-Verbindung erstellen
            webSocketConnection = BrowserConnector.getConnection(browserType, websocketUrl, port);

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////

            System.out.println("Trying to connect to WebSocket..");
            this.webSocketConnection.connect();

            ///////////////////////////////////////////////////////////////////////////////////////////////////////////

            System.out.println("Starting the Session..");
            session = new Session(browserType, webSocketConnection);
            System.out.println("***** ***** ***** Session erstellt ***** ***** *****");


            ///////////////////////////////////////////////////////////////////////////////////////////////////////////
            // Print BiDi status of the browser
            System.out.println("Obtaining status..");
            String statusResponse = session.status().get();

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

    // ToDo: Check if "get" is standard conform
    public void get(String url) {
        try
        {
            session.getBrowsingContext(0).navigate(url); // ToDo: Check if index 0 is always the right one!
        }
        catch( ExecutionException | InterruptedException e)
        {
            // Todo
        }
    }

}
