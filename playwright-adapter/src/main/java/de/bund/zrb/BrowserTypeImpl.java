package de.bund.zrb;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;

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


    private final String[] cdpUrl = {null};
    private final String[] wdUrl = {null};
    private final PlaywrightImpl playwright; // Required for the Playwright interface to implement the close method
    private Process process;

    private final String name;

    // ToDo: Check this not playwrigth specific parameters:
    private final int defaultPort = 9222; // ToDo: Externalize to a configuration file
    private final String defaultUrl = "ws://127.0.0.1"; // ToDo: Externalize to a configuration file
    private final String browserPath;
    private String profilePath;
    private String websocketUrl;
    private final String webSocketEndpoint;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Constructors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private BrowserTypeImpl(PlaywrightImpl playwright, String name, String browserPath, String profilePath, String webSocketEndpoint) {
        this.playwright = playwright;
        this.name = name;
        this.browserPath = browserPath;
        this.profilePath = profilePath;
        this.webSocketEndpoint = webSocketEndpoint;
        this.websocketUrl = defaultUrl + ":" + defaultPort; // Default WebSocket URL
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /// Static Factory Methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public static BrowserTypeImpl newFirefoxInstance(PlaywrightImpl playwright) {
        return new BrowserTypeImpl(playwright, "firefox","C:\\Program Files\\Mozilla Firefox\\firefox.exe", "C:\\FirefoxProfile", "/session");
    }

    public static BrowserTypeImpl newChromiumInstance(PlaywrightImpl playwright) {
        return new BrowserTypeImpl(playwright,"chromium" ,"C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe", "C:\\ChromeProfile", "");
    }

    public static BrowserTypeImpl newEdgeInstance(PlaywrightImpl playwright) {
        return new BrowserTypeImpl(playwright,"edge","C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe", "C:\\EdgeProfile", "");
    }

    public static BrowserTypeImpl newWebkitInstance(PlaywrightImpl playwright) {
        return new BrowserTypeImpl(playwright,"webkit",null, null, "");
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
        if (options == null) {
            options = new LaunchOptions();
        }

        try {
            startProcess(getCommandLineArgs(options));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            ConnectOptions connectOptions = new ConnectOptions();
            connectOptions.timeout = options.timeout;
            return connect(websocketUrl + webSocketEndpoint, connectOptions);
        } catch (Exception e) {
            process.destroyForcibly(); // Erzwinge den Stopp
            throw e;
        }

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
        WebSocketImpl webSocketImpl = new WebSocketImpl(URI.create(wsEndpoint), options.timeout);
        try {
            webSocketImpl.connect();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            BrowserImpl browser = new BrowserImpl(this, process, webSocketImpl); // PlayWright API forces Browser to know BrowserType, process may be optional!
            playwright.addBrowser(browser); // May be optional!
            return browser;
        } catch (ExecutionException | InterruptedException e) {
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
    protected void startProcess(List<String> commandLineArgs) throws Exception {
        String logPrefix = "[" + name() + "]";
        ProcessBuilder builder = new ProcessBuilder(commandLineArgs);

        builder.redirectErrorStream(true);
//        builder.inheritIO(); // Vererbt die IO-Streams des Elternprozesses
        process = builder.start();

        // Log-Ausgabe asynchron in einem separaten Thread
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(logPrefix + " " + line);

                    // CDP-URL aus der Log-Ausgabe extrahieren
                    if (line.contains("DevTools listening on ws://")) {
                        String url = line.substring(line.indexOf("ws://")).trim();
                        synchronized (cdpUrl) {
                            cdpUrl[0] = url; // Speichere die URL
                        }
                    }

                    // WebDriver-URL aus der Log-Ausgabe extrahieren
                    if (line.contains("WebDriver BiDi listening on ws://")) {
                        String url = line.substring(line.indexOf("ws://")).trim();
                        synchronized (wdUrl) {
                            wdUrl[0] = url; // Speichere die URL
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Lesen der Prozess-Ausgabe: " + e.getMessage());
            }
        }).start();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (this.name.equalsIgnoreCase("FIREFOX")) {
            // Warte auf die WebSocket-URL, falls erforderlich
            for (int i = 0; i < 10; i++) { // Maximal 10 Sekunden warten
                synchronized (wdUrl) {
                    if (wdUrl[0] != null) {
                        break;
                    }
                }
                Thread.sleep(1000); // Warte 1 Sekunde
            }
            System.out.println("Gefundene WebDriver-URL: " + wdUrl[0]);
            websocketUrl = wdUrl[0];
        }
        else
        {
            for (int i = 0; i < 10; i++) { // Maximal 10 Sekunden warten
                synchronized (cdpUrl) {
                    if (cdpUrl[0] != null) {
                        break;
                    }
                }
                Thread.sleep(1000); // Warte 1 Sekunde
            }
            System.out.println("Gefundene DevTools-URL: " + cdpUrl[0]);
//            websocketUrl = cdpUrl[0];
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    /////////////////////////////////////////////////////////////////////////////

    private String getCdpUrl() {
        synchronized (cdpUrl) {
            return cdpUrl[0];
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////



    ///////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Launch Features
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // Argumente für den Start des Browsers dynamisch zusammenstellen
    // ToDo: Port is only set via UI, but is still found in BrowserTypeImpl
    protected List<String> getCommandLineArgs(LaunchOptions options) {
        if (options.args == null || options.args.isEmpty()) {
            throw new IllegalArgumentException("Keine Startargumente gesetzt. Stelle sicher, dass die UI-Optionen korrekt übergeben werden.");
        }

        List<String> args = new ArrayList<>();
        args.add(browserPath); // Browser-Executable hinzufügen
        args.addAll(options.args); // Alle Argumente aus der UI übernehmen

        // Headless-Mode explizit setzen, wenn erforderlich
        if (Boolean.TRUE.equals(options.headless)) {
            args.add("--headless");
        }

        return args;
    }

}