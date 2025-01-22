package wd4j.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public enum BrowserType {

    FIREFOX {
        @Override
        public void launch(int port) throws Exception {
            String firefoxPath = "C:\\Program Files\\Mozilla Firefox\\firefox.exe";
            String firefoxProfileDirectory = "C:\\FirefoxProfile";

            Process process = new ProcessBuilder(
                    firefoxPath,
                    "--remote-debugging-port=" + port, // WebSocket-Server aktivieren
                    "--no-remote"//,                  // Verhindert Konflikte mit laufenden Instanzen
//                    "--profile", firefoxProfileDirectory,  // Optional: Benutzerdefiniertes Profil
//                    "--headless"                    // Optional: Headless-Modus für Tests
            )
                    .redirectErrorStream(true) // Kombiniert stdout und stderr
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Firefox Log] " + line);
                }
            }

            System.out.println("Firefox gestartet für WebDriver BiDi.");
        }
    },

    CHROME {
        @Override
        public void launch(int port) throws Exception {
            String chromePath = "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe";

            Process process = new ProcessBuilder(
                    chromePath,
                    "--remote-debugging-port=" + port,
                    "--user-data-dir=C:\\ChromeProfile", // Benutzerdefiniertes Profil
                    "--disable-gpu",                    // Optional: Deaktiviert GPU-Beschleunigung
                    "--headless"                        // Optional: Headless-Modus
            )
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Chrome Log] " + line);
                }
            }

            System.out.println("Chrome gestartet für WebDriver BiDi.");
        }
    },

    EDGE {
        @Override
        public void launch(int port) throws Exception {
            String edgePath = "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe";

            Process process = new ProcessBuilder(
                    edgePath,
                    "--remote-debugging-port=" + port,
                    "--user-data-dir=C:\\EdgeProfile"
            )
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[Edge Log] " + line);
                }
            }

            System.out.println("Edge gestartet für WebDriver BiDi.");
        }
    },

    SAFARI {
        @Override
        public void launch(int port) throws Exception {
            System.out.println("Safari-WebDriver-Integration ist derzeit nur auf macOS möglich.");
        }
    };

    // Abstrakte Methode, die von jedem Enum-Typ implementiert wird
    public abstract void launch(int port) throws Exception;
}
