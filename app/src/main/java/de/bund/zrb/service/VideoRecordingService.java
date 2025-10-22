package de.bund.zrb.service;

import com.sun.jna.platform.win32.WinDef;
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.config.VideoConfig;
import de.bund.zrb.video.WindowRecorder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

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

    /**
     * Integration-Hook aus der App-Schicht.
     * - Ermittelt das HWND aus dem BrowserImpl (lazy, cached dort)
     * - Setzt das Ziel-Fenster
     * - Startet optional automatisch, falls in VideoConfig aktiviert
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
            autostartIfConfigured();
        } catch (Throwable t) {
            System.err.println("[Video] init() Fehler: " + t.getMessage());
        }
    }

    /** Versucht einen Auto-Start, wenn in den Settings aktiviert; idempotent. */
    public synchronized void autostartIfConfigured() {
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
    public synchronized void setTargetWindow(WinDef.HWND hwnd) {
        this.targetWindow = hwnd;
    }

    /** Optionaler Getter (falls benötigt). */
    public WinDef.HWND getTargetWindow() { return targetWindow; }

    /** Läuft eine Aufnahme? */
    public boolean isRecording() { return current != null; }

    /** Startet die Aufnahme (falls nicht bereits laufend). */
    public synchronized void start() throws Exception {
        if (current != null) return;
        WinDef.HWND hwnd = this.targetWindow;
        if (hwnd == null) {
            throw new IllegalStateException("Kein Ziel-Fenster gesetzt (targetWindow == null).");
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
}
