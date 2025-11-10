package de.bund.zrb.bootstrap;

import de.bund.zrb.service.RecorderEventBridge;
import de.bund.zrb.service.SettingsService;

/**
 * Zentraler Bootstrap für nicht-UI-Initialisierungen der App.
 * - RecorderEventBridge installieren (Event-Wiring)
 * - Adapter/Settings initialisieren
 * - ShutdownHook (AppMap Info) registrieren
 */
public final class AppBootstrap {
    private static volatile boolean initialized = false;

    private AppBootstrap() {}

    public static synchronized void initialize() {
        if (initialized) return;
        // Recorder/Events
        RecorderEventBridge.install();
        // Adapter/Proxies etc.
        SettingsService.initAdapter();
        // Info-Hook (früher in Main.installInfoShutdownHook)
        installInfoShutdownHook();
        initialized = true;
    }

    private static void installInfoShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            String baseDir = System.getProperty("user.dir");
            String recordingDir = baseDir + java.io.File.separator + "tmp" + java.io.File.separator + "appmap";
            System.out.println("AppMap recordings directory: " + recordingDir);
        }));
    }
}

