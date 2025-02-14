package wd4j.impl.playwright;

import wd4j.api.Browser;
import wd4j.api.BrowserContext;
import wd4j.api.BrowserType;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * This class has a lot of methods that are not part of the Playwright API. This is due to the fact that the W3C stamdard
 * is not complete in all aspects. Therefore, some methods are added to this class to make the implementation more
 * flexible and to be able to use the full functionality of the underlying browser.
 *
 * This class especially encapsulates the physical connection, it may better be moved to a service class in the future?
 */
public class BrowserTypeImpl implements BrowserType {

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Fields
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    final String[] devToolsUrl = {null};
    private final PlaywrightImpl playwright; // Required for the Playwright interface to implement the close method
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

    private BrowserTypeImpl(PlaywrightImpl playwright, String name, String browserPath, String profilePath, String webSocketEndpoint, boolean headless, boolean noRemote, boolean disableGpu, boolean startMaximized) {
        this.playwright = playwright;
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

    public static BrowserTypeImpl newFirefoxInstance(PlaywrightImpl playwright) {
        return new BrowserTypeImpl(playwright, "firefox","C:\\Program Files\\Mozilla Firefox\\firefox.exe", "C:\\FirefoxProfile", "/session", false, true, false, false);
    }

    public static BrowserTypeImpl newChromiumInstance(PlaywrightImpl playwright) {
        return new BrowserTypeImpl(playwright,"chromium" ,"C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe", "C:\\ChromeProfile", "", true, false, true, true);
    }

    public static BrowserTypeImpl newEdgeInstance(PlaywrightImpl playwright) {
        return new BrowserTypeImpl(playwright,"edge","C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe", "C:\\EdgeProfile", "", true, false, true, false);
    }

    public static BrowserTypeImpl newWebkitInstance(PlaywrightImpl playwright) {
        return new BrowserTypeImpl(playwright,"webkit",null, null, "", false, false, false, false);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    /**
     * Launches a browser instance and connects to it.
     * @return The browser instance
     */
    @Override
    public Browser launch() {
        return launch(null);
    }

    /**
     * Launches a browser instance with the given options and connects to it.
     *
     * @param options
     * @return The browser instance
     */
    @Override
    public Browser launch(LaunchOptions options) {

        // ToDo: Use Options for Headless (OPTIONAL: NoRemote, DisableGpu, StartMaximized)
        if(options != null) {
            setParams(null, null, options.headless, null, null);
        }

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

    // ToDo: Obwohl diese Methode "connect" heißt, wird hier eigentlich nur ein Browser-Objekt erstellt und zurückgegeben
    //  D.h. dass der Browser gestartet wird; die WebSocket-Verbindung muss im Prinzip hier nicht aufgebaut werden
    //  (sondern erst bei der Verwendung des Browser-Objekts) Allerdings hat die Methode "connect" in der Playwright-API
    //  die Parameter "wsEndpoint" und "options" die für die WebSocket-Verbindung benötigt werden. Diese Werte könnten
    //
    @Override
    public Browser connect(String wsEndpoint, ConnectOptions options) {
        WebSocketImpl webSocketImpl = new WebSocketImpl();
        webSocketImpl.createAndConfigureWebSocketClient(URI.create(wsEndpoint));
        try {
            webSocketImpl.connect();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            BrowserImpl browser = new BrowserImpl(this, webSocketImpl);
            playwright.addBrowser(browser);
            return browser;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
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

    private String getDevToolsUrl() {
        synchronized (devToolsUrl) {
            return devToolsUrl[0];
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    /////

    private Integer getPort() {
        return port;
    }

    private String getWebsocketEndpoint() {
        return webSocketEndpoint;
    }

    private String getProfilePath() {
        return profilePath;
    }

    private boolean isNoRemote() {
        return noRemote;
    }

    private boolean isDisableGpu() {
        return disableGpu;
    }

    private boolean isStartMaximized() {
        return startMaximized;
    }

    private boolean isHeadless() {
        return headless;
    }

    ///////////////////////////////////////////////////////////////////////////

    private void setPort(Integer port) {
        this.port = port;
    }

    private void setProfilePath(String profilePath) {
        this.profilePath = profilePath;
    }

    private void setNoRemote(boolean noRemote) {
        this.noRemote = noRemote;
    }

    private void setDisableGpu(boolean disableGpu) {
        this.disableGpu = disableGpu;
    }

    private void setStartMaximized(boolean startMaximized) {
        this.startMaximized = startMaximized;
    }

    private void setHeadless(boolean headless) {
        this.headless = headless;
    }

    ///////////////////////////////////////////////////////////////////////////

    private void setParams(Integer port, String profilePath, Boolean headless, Boolean disableGpu, Boolean noRemote) {

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
    // ToDo: Needs to be updated to the Playwright API Options
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