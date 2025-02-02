package wd4j.impl;

import wd4j.api.APIRequest;
import wd4j.api.BrowserType;
import wd4j.api.Playwright;
import wd4j.api.Selectors;

import java.util.List;
import java.util.ArrayList;

/**
 * Wrappes the WebSocketConnection and is responsible for options and browser types.
 */
public class PlaywrightImpl implements Playwright {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Fields
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    List<WebSocketImpl> connections = new ArrayList<>(); //  List of all connections, required for cleanup

    // Felder für die Konfigurationsoptionen (not Playwright specific):
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

    private PlaywrightImpl(CreateOptions options) {
        // ToDo: Implement options
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Required for the Playwright interface (used in the PlaywrightFactory via reflection):
    public static Playwright create(CreateOptions options) {
        return new PlaywrightImpl(options);
    }

    @Override
    public BrowserType chromium() {
        WebSocketImpl connection = new WebSocketImpl();
        // ToDo: Might be better encapsulated
        connections.add(connection); // Since BrowserType has a connect() but no disconnect() or close() method
        BrowserTypeImpl browserType = BrowserTypeImpl.newChromiumInstance(connection);
        return browserType;
    }

    @Override
    public BrowserType firefox() {
        WebSocketImpl connection = new WebSocketImpl();
        // ToDo: Might be better encapsulated
        connections.add(connection); // Since BrowserType has a connect() but no disconnect() or close() method
        BrowserTypeImpl browserType =  BrowserTypeImpl.newFirefoxInstance(connection);
        return browserType;
    }

    @Override
    public BrowserType webkit() {
        WebSocketImpl connection = new WebSocketImpl();
        // ToDo: Might be better encapsulated
        connections.add(connection); // Since BrowserType has a connect() but no disconnect() or close() method
        BrowserTypeImpl browserType =  BrowserTypeImpl.newWebkitInstance(connection);
        return browserType;
    }

    @Override
    public APIRequest request() {
        throw new UnsupportedOperationException("APIRequest not yet implemented");
    }

    @Override
    public Selectors selectors() {
        throw new UnsupportedOperationException("Selectors not yet implemented");
    }

    @Override
    public void close() {
        // Close all BrowserTypes
        for (WebSocketImpl connection : connections) {
            connection.close();
        }
    }
}
