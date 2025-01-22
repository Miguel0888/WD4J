package wd4j.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public enum BrowserType {
    FIREFOX("C:\\Program Files\\Mozilla Firefox\\firefox.exe", "C:\\FirefoxProfile", false, true, false, false) {
        @Override
        public Process launch(int port) throws Exception {
            List<String> args = buildArguments(port);
            ProcessBuilder builder = new ProcessBuilder(args);

            return startProcess(builder, "[Firefox Log]");
        }
    },

    CHROME("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe", "C:\\ChromeProfile", true, false, true, true) {
        @Override
        public Process launch(int port) throws Exception {
            List<String> args = buildArguments(port);
            ProcessBuilder builder = new ProcessBuilder(args);

            return startProcess(builder, "[Chrome Log]");
        }
    },

    EDGE("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe", "C:\\EdgeProfile", true, false, true, false) {
        @Override
        public Process launch(int port) throws Exception {
            List<String> args = buildArguments(port);
            ProcessBuilder builder = new ProcessBuilder(args);

            return startProcess(builder, "[Edge Log]");
        }
    },

    SAFARI(null, null, false, false, false, false) {
        @Override
        public Process launch(int port) throws Exception {
            System.out.println("Safari-WebDriver-Integration ist derzeit nur auf macOS möglich.");
            return null;
        }
    };

    // Felder für die Konfigurationsoptionen
    protected final String browserPath;
    protected String profilePath;
    protected boolean headless;
    protected boolean noRemote;
    protected boolean disableGpu;
    protected final boolean startMaximized;

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
    public abstract Process launch(int port) throws Exception;

    // Hilfsmethode zum Starten eines Prozesses mit Protokollierung
    protected Process startProcess(ProcessBuilder builder, String logPrefix) throws Exception {
        builder.redirectErrorStream(true);
        Process process = builder.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(logPrefix + " " + line);
            }
        }

        return process;
    }

    // Argumente für den Start des Browsers dynamisch zusammenstellen
    protected List<String> buildArguments(int port) {
        List<String> args = new ArrayList<>();
        args.add(browserPath);

        if (port > 0) {
            args.add("--remote-debugging-port=" + port);
        }
        if (noRemote) {
            args.add("--no-remote");
        }
        if (profilePath != null) {
            args.add(browserPath.endsWith("firefox.exe") ? "--profile=" + profilePath : "--user-data-dir=" + profilePath);
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
