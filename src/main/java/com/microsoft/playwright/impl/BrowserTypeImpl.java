package com.microsoft.playwright.impl;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import wd4j.core.WebSocketConnection;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BrowserTypeImpl implements BrowserType {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Fields
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final WebSocketConnection webSocketConnection;
    final String[] devToolsUrl = {null};
    private Process process;

    private final String name;

    // ToDo: Check this not playwrigth specific parameters:
    private int port = 9222;
    private final String browserPath;
    private String profilePath;
    private final String webSocketEndpoint;
    @Deprecated // ToDo: Remove since it is part of the playwright API
    private boolean headless;
    private boolean noRemote;
    private boolean disableGpu;
    private boolean startMaximized;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public BrowserTypeImpl(WebSocketConnection webSocketConnection, String name, String browserPath, String profilePath, String webSocketEndpoint, boolean headless, boolean noRemote, boolean disableGpu, boolean startMaximized) {
        this.webSocketConnection = webSocketConnection;
        this.name = name;
        this.browserPath = browserPath;
        this.profilePath = profilePath;
        this.webSocketEndpoint = webSocketEndpoint;
        this.headless = headless;
        this.noRemote = noRemote;
        this.disableGpu = disableGpu;
        this.startMaximized = startMaximized;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Static Factory Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static BrowserTypeImpl newFirefoxInstance(WebSocketConnection webSocketConnection) {
        return new BrowserTypeImpl( webSocketConnection,"firefox","C:\\Program Files\\Mozilla Firefox\\firefox.exe", "C:\\FirefoxProfile", "/session", false, true, false, false);
    }

    public static BrowserTypeImpl newChromiumInstance(WebSocketConnection webSocketConnection) {
        return new BrowserTypeImpl(webSocketConnection,"chromium" ,"C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe", "C:\\ChromeProfile", "", true, false, true, true);
    }

    public static BrowserTypeImpl newEdgeInstance(WebSocketConnection webSocketConnection) {
        return new BrowserTypeImpl(webSocketConnection,"edge","C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe", "C:\\EdgeProfile", "", true, false, true, false);
    }

    public static BrowserTypeImpl newWebkitInstance(WebSocketConnection webSocketConnection) {
        return new BrowserTypeImpl(webSocketConnection,"webkit",null, null, "", false, false, false, false);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public Browser launch() {
        try {
            startProcess();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return connect(getDevToolsUrl(), null);
    }

    @Override
    public Browser launch(LaunchOptions options) {

        // ToDo: Use Options for Headless (OPTIONAL: NoRemote, DisableGpu, StartMaximized)
        try {
            startProcess();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String websocketUrl = "ws://127.0.0.1:" + port;
        return connect(websocketUrl + webSocketEndpoint, null);
    }

    @Override
    public BrowserContext launchPersistentContext(Path userDataDir, LaunchPersistentContextOptions options) {
        throw new UnsupportedOperationException("Persistent contexts not supported yet");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Browser connect(String wsEndpoint, ConnectOptions options) {
        webSocketConnection.createAndConfigureWebSocketClient(URI.create(wsEndpoint));
        try {
            webSocketConnection.connect();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return new BrowserImpl(this);
    }

    @Override
    public Browser connectOverCDP(String endpointURL, ConnectOverCDPOptions options) {
        throw new UnsupportedOperationException("CDP connection not supported yet");
    }

    @Override
    public String executablePath() {
        return browserPath;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Optional Parameters (not part of the playwright API)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Hilfsmethode zum Starten eines Prozesses mit Protokollierung
    protected void startProcess() throws Exception {
        String logPrefix = "[" + name() + "]";
        List<String> commandLineArgs = getCommandLineArgs();
        ProcessBuilder builder = new ProcessBuilder(commandLineArgs);

        builder.redirectErrorStream(true);
        process = builder.start();

        // Log-Ausgabe asynchron in einem separaten Thread
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(logPrefix + " " + line);

                    // WebSocket-URL aus der Log-Ausgabe extrahieren
                    if (line.contains("DevTools listening on ws://")) {
                        String url = line.substring(line.indexOf("ws://")).trim();
                        synchronized (devToolsUrl) {
                            devToolsUrl[0] = url; // Speichere die URL
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Lesen der Prozess-Ausgabe: " + e.getMessage());
            }
        }).start();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (!this.name.equalsIgnoreCase("FIREFOX")) {
            // Warte auf die WebSocket-URL, falls erforderlich
            for (int i = 0; i < 10; i++) { // Maximal 10 Sekunden warten
                synchronized (devToolsUrl) {
                    if (devToolsUrl[0] != null) {
                        break;
                    }
                }
                Thread.sleep(1000); // Warte 1 Sekunde
            }

            System.out.println("Gefundene DevTools-URL: " + devToolsUrl[0]);
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    public void terminateProcess(Process process) {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }

    /////////////////////////////////////////////////////////////////////////////

    public String getDevToolsUrl() {
        synchronized (devToolsUrl) {
            return devToolsUrl[0];
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public WebSocketConnection getWebSocketConnection() {
        return webSocketConnection;
    }

    /////

    public Integer getPort() {
        return port;
    }

    public String getWebsocketEndpoint() {
        return webSocketEndpoint;
    }

    public String getProfilePath() {
        return profilePath;
    }

    public boolean isNoRemote() {
        return noRemote;
    }

    public boolean isDisableGpu() {
        return disableGpu;
    }

    public boolean isStartMaximized() {
        return startMaximized;
    }

    public boolean isHeadless() {
        return headless;
    }

    ///////////////////////////////////////////////////////////////////////////

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setProfilePath(String profilePath) {
        this.profilePath = profilePath;
    }

    public void setNoRemote(boolean noRemote) {
        this.noRemote = noRemote;
    }

    public void setDisableGpu(boolean disableGpu) {
        this.disableGpu = disableGpu;
    }

    public void setStartMaximized(boolean startMaximized) {
        this.startMaximized = startMaximized;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    ///////////////////////////////////////////////////////////////////////////

    public void setParams(Integer port, String profilePath, Boolean headless, Boolean disableGpu, Boolean noRemote) {

        if( port != null) {
            this.port = port;
        }
        if (profilePath != null) {
            this.profilePath = profilePath;
        }
        if (headless != null) {
            this.headless = headless;
        }
        if (disableGpu != null) {
            this.disableGpu = disableGpu;
        }
        if (noRemote != null) {
            this.noRemote = noRemote;
        }
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Launch Features
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Argumente für den Start des Browsers dynamisch zusammenstellen
    protected List<String> getCommandLineArgs() {
        List<String> args = new ArrayList<>();
        args.add(browserPath);

        if (port > 0) {
            args.add("--remote-debugging-port=" + port);
        }
        if (noRemote) {
            args.add("--no-remote");
        }
        if( profilePath == null) {
            // Kein Profil-Argument hinzufügen, wenn profilePath null oder leer ist
            System.out.println("Kein Profil angegeben, der Browser wird ohne Profil gestartet.");
        }
        else if (!profilePath.isEmpty()) {
            args.add(browserPath.endsWith("firefox.exe") ? "--profile=" + profilePath : "--user-data-dir=" + profilePath);
        }
        else {
            // Temporäres Profil verwenden
            String tempProfilePath = System.getProperty("java.io.tmpdir") + "\\temp_profile_" + System.currentTimeMillis();
            args.add(browserPath.endsWith("firefox.exe") ? "--profile=" + tempProfilePath : "--user-data-dir=" + tempProfilePath);

            // Optional: Log für Debugging
            System.out.println("Kein Profil angegeben, temporäres Profil wird verwendet: " + tempProfilePath);
        }
        if (headless) {
            args.add("--headless");
        }
        if (disableGpu) {
            args.add("--disable-gpu");
        }
        if (startMaximized) {
            args.add("--start-maximized");
        }
//        if (!useCdp) {
////            args.add("--remote-debugging-address=127.0.0.1");
//        }

        return args;
    }
}