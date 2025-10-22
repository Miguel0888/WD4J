package de.bund.zrb.config;

/**
 * Zentrale, adapter-taugliche Recording-Konfiguration.
 * Wird NICHT aus der UI geladen – dafür ist die App-Schicht zuständig.
 * Diese Klasse bietet nur statische, volatile Werte + einfache Helper.
 */
public final class VideoConfig {

    // Defaults
    private static final boolean DEFAULT_ENABLED   = true;
    private static final int     DEFAULT_FPS       = 15;
    private static final String  DEFAULT_REPORTS   = "C:/Recordings";

    // Konfigurierbare Felder (thread-safe lesbar/schreibbar)
    private static volatile boolean enabled   = DEFAULT_ENABLED;
    private static volatile int     fps       = DEFAULT_FPS;
    private static volatile String  reportsDir = DEFAULT_REPORTS;

    private VideoConfig() {}

    // --- Getter ---
    public static boolean isEnabled() { return enabled; }
    public static int getFps() { return fps; }
    public static String getReportsDir() { return reportsDir; }

    // --- Setter ---
    public static void setEnabled(boolean value) { enabled = value; }
    public static void setFps(int value) {
        if (value <= 0) throw new IllegalArgumentException("fps must be > 0");
        fps = value;
    }
    public static void setReportsDir(String dir) {
        if (dir == null || dir.trim().isEmpty()) {
            throw new IllegalArgumentException("reportsDir must not be empty");
        }
        reportsDir = dir.trim();
    }

    // --------------------------------------------------------------------------------------------
    // Optionale Helper – nutzbar aus der App-Schicht
    // --------------------------------------------------------------------------------------------

    /**
     * Initialisiert Felder aus System Properties (falls gesetzt).
     * Namen:
     *  - recording.enabled   (true/false)
     *  - recording.fps       (int > 0)
     *  - recording.reportsDir (Pfad)
     */
    public static void initFromSystemProperties() {
        String pEnabled = System.getProperty("recording.enabled");
        if (pEnabled != null) {
            setEnabled(Boolean.parseBoolean(pEnabled));
        }
        String pFps = System.getProperty("recording.fps");
        if (pFps != null) {
            try { setFps(Integer.parseInt(pFps)); } catch (NumberFormatException ignored) {}
        }
        String pDir = System.getProperty("recording.reportsDir");
        if (pDir != null && !pDir.trim().isEmpty()) {
            setReportsDir(pDir);
        }
    }

    /**
     * Setzt alle Werte in einem Rutsch. Null bedeutet: Wert bleibt unverändert.
     */
    public static void configure(Boolean enabledOpt, Integer fpsOpt, String reportsDirOpt) {
        if (enabledOpt != null) setEnabled(enabledOpt);
        if (fpsOpt != null) setFps(fpsOpt);
        if (reportsDirOpt != null) setReportsDir(reportsDirOpt);
    }

    /**
     * Setzt wieder auf Defaults zurück (praktisch für Tests).
     */
    public static void resetToDefaults() {
        enabled = DEFAULT_ENABLED;
        fps = DEFAULT_FPS;
        reportsDir = DEFAULT_REPORTS;
    }

    @Override
    public String toString() {
        return "RecordingConfig{" +
                "enabled=" + enabled +
                ", fps=" + fps +
                ", reportsDir='" + reportsDir + '\'' +
                '}';
    }
}
