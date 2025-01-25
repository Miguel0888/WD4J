package wd4j.core;

import wd4j.helper.BrowserConnector;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import wd4j.api.By;
import wd4j.api.WebDriver;
import wd4j.api.WebElement;
import wd4j.helper.BrowserType;
import wd4j.helper.JsonObjectBuilder;
import wd4j.impl.modules.BrowsingContext;
import wd4j.impl.modules.Session;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class BiDiWebDriver implements WebDriver {
    private final Process browserProcess; // The Handle to the Browser Process
    private final String websocketUrl;
    private WebSocketConnection webSocketConnection = null;
    private final Session session;
    private final BrowserType browserType;
    private final int port;

    public BiDiWebDriver(BrowserType browserType) {
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

    @Override
    public void close() {
        session.endSession();
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // ToDo: Move Overrides to Session ?
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public WebElement findElement(By locator) {
        return null;
    }

    // ToDo: Check if "get" is standard conform
    @Override
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

    @Override
    public String getCurrentUrl() {
        return "";
    }

    @Override
    public String getTitle() {
        return "";
    }

}
