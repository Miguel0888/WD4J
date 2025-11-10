package de.bund.zrb.service;

import com.sun.jna.platform.win32.WinDef;
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.config.VideoConfig;
import de.bund.zrb.video.WindowRecorder;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Kleiner Singleton zum manuellen Start/Stop der Fenster-Videoaufnahme.
 * Nutzt WindowRecorder + VideoConfig (fps, Container/Optionen, Zielordner).
 * Auto-Start wird hier im Service abgewickelt (init() + autostartIfConfigured()).
 */
public final class VideoRecordingService {

    private static final VideoRecordingService INSTANCE = new VideoRecordingService();
    public static VideoRecordingService getInstance() { return INSTANCE; }

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private volatile WinDef.HWND targetWindow; // muss gesetzt sein (per Integration)
    private volatile WindowRecorder current;

    private VideoRecordingService() {}

    private boolean videoStackAvailable() {
        if (checkClassesOnCurrentLoader()) return true;
        Path libDir = settingsLibDir();
        if (libDir == null) {
            System.out.println("[Video] Kein settings libDir verfügbar.");
            return false;
        }
        if (!Files.isDirectory(libDir)) {
            System.out.println("[Video] libDir existiert nicht: " + libDir);
            return false;
        }
        try {
            File[] jarFiles = libDir.toFile().listFiles((dir, name) -> name != null && name.toLowerCase().endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                System.out.println("[Video] Keine JARs in " + libDir);
                return false;
            }
            System.out.println("[Video] Gefundene JARs in libDir:" );
            for (File f : jarFiles) System.out.println("[Video]  - " + f.getName());
            List<URL> urls = new ArrayList<>();
            for (File f : jarFiles) urls.add(f.toURI().toURL());
            attachUrls(urls);
        } catch (Throwable t) {
            System.out.println("[Video] Attach Fehler: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        boolean ok = checkClassesOnCurrentLoader();
        System.out.println("[Video] Klassen nach Attach geladen: " + ok);
        return ok;
    }

    private boolean checkClassesOnCurrentLoader() {
        try {
            Class.forName("com.sun.jna.platform.win32.WinDef");
            Class.forName("org.bytedeco.javacv.FFmpegFrameRecorder");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Path settingsLibDir() {
        Path base = resolveSettingsDirSafe();
        if (base == null) return null;
        Path lib = base.resolve("lib");
        try { Files.createDirectories(lib); } catch (Throwable ignore) {}
        return lib;
    }

    private static Path resolveSettingsDirSafe() {
        // Versuche SettingsService-reflection
        try {
            Class<?> cls = Class.forName("de.bund.zrb.service.SettingsService");
            Method getInst = cls.getMethod("getInstance");
            Object inst = getInst.invoke(null);
            for (String m : new String[]{"getSettingsPath", "getWorkingDirectory", "getBasePath"}) {
                try {
                    Method gm = cls.getMethod(m);
                    Object r = gm.invoke(inst);
                    if (r instanceof String && !((String) r).trim().isEmpty()) {
                        Path p = Paths.get(((String) r).trim());
                        if (Files.exists(p)) return p;
                    }
                    if (r instanceof Path) {
                        Path p = (Path) r;
                        if (Files.exists(p)) return p;
                    }
                } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {}
        // Fallback
        try {
            Path p = Paths.get(System.getProperty("user.home"), ".wd4j");
            Files.createDirectories(p);
            return p;
        } catch (Throwable t) { return null; }
    }

    private static void attachUrls(List<URL> urls) throws Exception {
        // 1) Versuche SystemClassLoader (Java 8)
        ClassLoader sys = ClassLoader.getSystemClassLoader();
        tryAttachToUrlCl(sys, urls);
        // 2) Versuche Loader dieser Klasse
        ClassLoader own = VideoRecordingService.class.getClassLoader();
        if (own != sys) tryAttachToUrlCl(own, urls);
        // 3) Setze TCCL Child-Loader, damit abhängige Lookups funktionieren
        URLClassLoader child = new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(child);
    }

    private static void tryAttachToUrlCl(ClassLoader cl, List<URL> urls) {
        try {
            if (cl instanceof URLClassLoader) {
                Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addURL.setAccessible(true);
                for (URL u : urls) addURL.invoke(cl, u);
            }
        } catch (Throwable ignore) {}
    }

    /**
     * Integration-Hook aus der App-Schicht: setzt nur das Ziel-Fenster und versucht einen Auto-Start,
     * wenn der Video-Stack bereits verfügbar ist. Kein interaktives Nachladen an dieser Stelle
     * (um Prompts beim App-Start zu vermeiden).
     */
    public synchronized void init(BrowserImpl browser) {
        if (browser == null) {
            System.err.println("[Video] init(): BrowserImpl == null");
            return;
        }
        try {
            WinDef.HWND hwnd = browser.getTopLevelHwnd();
            if (hwnd == null) {
                System.err.println("[Video] init(): Kein Top-Level-Fenster gefunden (HWND=null).");
                return;
            }
            setTargetWindow(hwnd);
            if (videoStackAvailable()) {
                autostartIfConfigured();
            } else {
                System.out.println("[Video] Optionaler Video-Stack nicht vorhanden – Auto-Start wird übersprungen.");
            }
        } catch (Throwable t) {
            System.err.println("[Video] init() Fehler: " + t.getMessage());
        }
    }

    /** Versucht einen Auto-Start, wenn in den Settings aktiviert; idempotent. Lädt NICHT nach. */
    public synchronized void autostartIfConfigured() {
        if (!videoStackAvailable()) return; // kein Nachladen hier
        if (!VideoConfig.isEnabled()) return;
        if (isRecording()) return;
        try {
            start();
            System.out.println("[Video] Auto-Recording gestartet.");
        } catch (Throwable t) {
            System.err.println("[Video] Auto-Recording Start fehlgeschlagen: " + t.getMessage());
        }
    }

    /** Ziel-Fenster setzen (Integration von außen). */
    public synchronized void setTargetWindow(WinDef.HWND hwnd) { this.targetWindow = hwnd; }

    /** Optionaler Getter (falls benötigt). */
    public WinDef.HWND getTargetWindow() { return targetWindow; }

    /** Läuft eine Aufnahme? */
    public boolean isRecording() { return current != null; }

    /**
     * Startet die Aufnahme. Wenn der Video-Stack fehlt, wird der Nutzer hier (und nur hier)
     * nach einem Laufzeit-Download gefragt. Bei Ablehnung bleibt das Feature deaktiviert.
     */
    public synchronized void start() throws Exception {
        // HWND Lazy Versuch falls noch nicht gesetzt
        if (this.targetWindow == null) {
            System.out.println("[Video] Versuch HWND lazy zu ermitteln vor Start...");
            try {
                // BrowserImpl über UserContextMappingService ermitteln (falls vorhanden)
                // Fallback: kein Zugriff -> bleibt null
            } catch (Throwable ignore) {}
        }
        if (!videoStackAvailable()) {
            boolean ok = VideoRuntimeLoader.ensureVideoLibsAvailableInteractively();
            // Nach interaktivem Nachladen erneut versuchen ohne nochmal Dialog zu provozieren
            if (!ok && !videoStackAvailable()) {
                throw new IllegalStateException("Video-Stack nicht verfügbar (JARs fehlen oder inkompatibel)");
            }
        }
        if (current != null) return;
        WinDef.HWND hwnd = this.targetWindow;
        if (hwnd == null) {
            throw new IllegalStateException("Kein Ziel-Fenster gesetzt – Browser noch nicht bereit oder HWND-Auflösung fehlgeschlagen.");
        }

        // Ziel-Datei vorbereiten
        String baseDirStr = VideoConfig.getReportsDir();
        if (baseDirStr == null || baseDirStr.trim().isEmpty()) {
            baseDirStr = "C:/Reports";
        }
        Path baseDir = Paths.get(baseDirStr);
        try { Files.createDirectories(baseDir); } catch (Exception ignore) {}
        String ts = java.time.LocalDateTime.now().format(TS);
        Path outfile = baseDir.resolve("video-" + ts + ".mkv"); // WindowRecorder/Config kann Extension anpassen

        int fps = Math.max(1, VideoConfig.getFps());
        WindowRecorder wr = new WindowRecorder(hwnd, outfile, fps);
        wr.start();

        current = wr;
    }

    /** Stoppt die laufende Aufnahme (no-op, wenn keine aktiv). */
    public synchronized void stop() {
        WindowRecorder wr = current;
        current = null;
        if (wr != null) {
            try { wr.stop(); } catch (Throwable ignore) {}
        }
    }

    /** Startet oder stoppt je nach aktuellem Zustand. */
    public synchronized void toggle() throws Exception {
        if (isRecording()) stop(); else start();
    }

    /** Effektive Datei der aktuellen Aufnahme (falls laufend). */
    public Path getCurrentOutput() {
        WindowRecorder wr = current;
        return (wr != null) ? wr.getEffectiveOutput() : null;
    }

    static boolean quickCheckAvailable() {
        try {
            Class.forName("com.sun.jna.platform.win32.WinDef");
            Class.forName("org.bytedeco.javacv.FFmpegFrameRecorder");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
