package wd4j.helper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

// ToDo: Externalize browser paths and profile paths etc. to a configuration file
public enum BrowserType {
    FIREFOX("C:\\Program Files\\Mozilla Firefox\\firefox.exe", "C:\\FirefoxProfile", "/session",false, true, false, false) {
        @Override
        public Process launch() throws Exception {
            List<String> args = buildArguments();
            ProcessBuilder builder = new ProcessBuilder(args);

            return startProcess(builder, "[Firefox Log]");
        }
    },

    CHROME("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe", "C:\\ChromeProfile", "",true, false, true, true) {
        @Override
        public Process launch() throws Exception {
            List<String> args = buildArguments();
            ProcessBuilder builder = new ProcessBuilder(args);


          //  Use the command line flag --remote-allow-origins=http://localhost:9222 to allow connections from this origin or --remote-allow-origins=* to allow all origins.

            return startProcess(builder, "[Chrome Log]");
        }
    },

    EDGE("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe", "C:\\EdgeProfile", "",true, false, true, false) {
        @Override
        public Process launch() throws Exception {
            List<String> args = buildArguments();
            ProcessBuilder builder = new ProcessBuilder(args);

            return startProcess(builder, "[Edge Log]");
        }
    },

    SAFARI(null, null, "",false, false, false, false) {
        @Override
        public Process launch() throws Exception {
            System.out.println("Safari-WebDriver-Integration ist derzeit nur auf macOS möglich.");
            return null;
        }
    };

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

    // Konstruktor
    BrowserType(String browserPath, String profilePath, String webSocketEndpoint, boolean headless, boolean noRemote, boolean disableGpu, boolean startMaximized) {
        this.browserPath = browserPath;
        this.profilePath = profilePath;
        this.webSocketEndpoint = webSocketEndpoint;
        this.headless = headless;
        this.noRemote = noRemote;
        this.disableGpu = disableGpu;
        this.startMaximized = startMaximized;
    }

    // Abstrakte Methode für den Browserstart
    public abstract Process launch() throws Exception;

    // Hilfsmethode zum Starten eines Prozesses mit Protokollierung
    protected Process startProcess(ProcessBuilder builder, String logPrefix) throws Exception {
        builder.redirectErrorStream(true);
        Process process = builder.start();

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
        if (!this.name().equals("FIREFOX")) {
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

        return process;
    }

    public void terminateProcess(Process process) {
        if (process != null && process.isAlive()) {
            process.destroy();
        }
    }

    // Argumente für den Start des Browsers dynamisch zusammenstellen
    protected List<String> buildArguments() {
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
        if (useCdp) {
            args.add("--enable-blink-features=WebDriverBiDi");
        }

        return args;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getWebsocketEndpoint() {
        return webSocketEndpoint;
    }


    public void setProfilePath(String profilePath) {
        this.profilePath = profilePath;
    }

    public void setHeadless(boolean headless) {
        this.headless = headless;
    }

    public void setDisableGpu(boolean disableGpu) {
        this.disableGpu = disableGpu;
    }

    public void setNoRemote(boolean noRemote) {
        this.noRemote = noRemote;
    }

    public void setUseCdp(boolean useCdp) {
        this.useCdp = useCdp;
    }

    public boolean useCdp() {
        return useCdp;
    }

    public String getDevToolsUrl() {
        synchronized (devToolsUrl) {
            return devToolsUrl[0];
        }
    }
}
