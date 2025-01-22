package wd4j.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

// ToDo: Externalize browser paths and profile paths etc. to a configuration file
public enum BrowserType {
    FIREFOX("C:\\Program Files\\Mozilla Firefox\\firefox.exe", "C:\\FirefoxProfile", false, true, false, false) {
        @Override
        public Process launch() throws Exception {
            List<String> args = buildArguments();
            ProcessBuilder builder = new ProcessBuilder(args);

            return startProcess(builder, "[Firefox Log]");
        }
    },

    CHROME("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe", "C:\\ChromeProfile", true, false, true, true) {
        @Override
        public Process launch() throws Exception {
            List<String> args = buildArguments();
            ProcessBuilder builder = new ProcessBuilder(args);

            return startProcess(builder, "[Chrome Log]");
        }
    },

    EDGE("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe", "C:\\EdgeProfile", true, false, true, false) {
        @Override
        public Process launch() throws Exception {
            List<String> args = buildArguments();
            ProcessBuilder builder = new ProcessBuilder(args);

            return startProcess(builder, "[Edge Log]");
        }
    },

    SAFARI(null, null, false, false, false, false) {
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

    // Konstruktor
    BrowserType(String browserPath, String profilePath, boolean headless, boolean noRemote, boolean disableGpu, boolean startMaximized) {
        this.browserPath = browserPath;
        this.profilePath = profilePath;
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
                }
            } catch (Exception e) {
                System.err.println("Fehler beim Lesen der Prozess-Ausgabe: " + e.getMessage());
            }
        }).start();

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

        return args;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
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
}
