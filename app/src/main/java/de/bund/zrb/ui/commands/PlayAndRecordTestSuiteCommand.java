package de.bund.zrb.ui.commands;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.Severity;
import de.bund.zrb.event.StatusMessageEvent;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.TestPlayerService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.ui.tabs.RunnerPanel;
import de.bund.zrb.video.MediaRecorder;
import de.bund.zrb.video.MediaRuntimeBootstrap;
import de.bund.zrb.video.RecordingProfile;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Variante von PlayTestSuiteCommand, die automatisch eine Videoaufzeichnung
 * (gemäß Settings: Backend + FPS + Ausgabeordner) startet und nach Ende stoppt.
 * Nutzt das neue TestPlayerService.play() für Overlay-Unterstützung.
 */
public class PlayAndRecordTestSuiteCommand extends ShortcutMenuCommand {

    private final JTabbedPane tabbedPane;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    public PlayAndRecordTestSuiteCommand(JTabbedPane tabbedPane) {
        this.tabbedPane = tabbedPane;
    }

    @Override
    public String getId() { return "testsuite.play.record"; }

    @Override
    public String getLabel() { return "Testsuite ausführen + Video"; }

    @Override
    public void perform() {
        ApplicationEventBus.getInstance().publish(new StatusMessageEvent("🎬 Starte Aufnahme + Playback…", 2000));

        final RunnerPanel runnerPanel = new RunnerPanel();
        TestPlayerService.getInstance().registerLogger(runnerPanel.getLogger());

        final AtomicBoolean running = new AtomicBoolean(false);
        final AtomicBoolean recordingActive = new AtomicBoolean(false);

        tabbedPane.addTab("Runner", runnerPanel);
        final int tabIndex = tabbedPane.indexOfComponent(runnerPanel);
        tabbedPane.setTabComponentAt(tabIndex, createClosableTabHeader("Runner", tabbedPane, runnerPanel, running, recordingActive));
        tabbedPane.setSelectedComponent(runnerPanel);

        new Thread(() -> {
            running.set(true);
            MediaRecorder recorder = null;
            try {
                // Recorder erstellen (Backend aus Settings)
                recorder = MediaRuntimeBootstrap.createRecorder();
                RecordingProfile profile = buildProfile();
                recorder.start(profile);
                recordingActive.set(true);
                ApplicationEventBus.getInstance().publish(new StatusMessageEvent("● Aufnahme läuft", 1500));

                // Testdurchlauf mit Overlay
                TestPlayerService.getInstance().play(null); // null -> verwendet Auswahl oder Root intern

                ApplicationEventBus.getInstance().publish(new StatusMessageEvent("✔ Playback fertig", 2000));
            } catch (Throwable t) {
                String msg = (t.getMessage() == null) ? t.toString() : t.getMessage();
                ApplicationEventBus.getInstance().publish(new StatusMessageEvent("Fehler: " + msg, 5000, Severity.ERROR));
            } finally {
                // Aufnahme stoppen
                if (recorder != null) {
                    try { recorder.stop(); } catch (Throwable ignore) {}
                }
                if (recordingActive.get()) {
                    ApplicationEventBus.getInstance().publish(new StatusMessageEvent("⏹ Aufnahme gestoppt", 2000));
                }
                recordingActive.set(false);
                running.set(false);
            }
        }, "WD4J-Runner-Record").start();
    }

    private RecordingProfile buildProfile() throws Exception {
        SettingsService s = SettingsService.getInstance();
        // Quelle: Bildschirm (screen://). Für VLC kann Region/FPS in Settings stehen.
        String src = "screen://";
        int fps = orInt(s.get("video.fps", Integer.class), 15);
        int w = orInt(s.get("video.width", Integer.class), 0);
        int h = orInt(s.get("video.height", Integer.class), 0);
        if (w <= 0 || h <= 0) {
            Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
            w = (int) d.getWidth();
            h = (int) d.getHeight();
        }
        String codec = orString(s.get("video.codec", String.class), "h264");
        // Audio (nur relevant für VLC / optional JCodec experimentell)
        String backend = orString(s.get("video.backend", String.class), "jcodec").toLowerCase();

        Path baseDir = resolveOutputDir();
        String ts = java.time.LocalDateTime.now().format(TS);
        Path out = baseDir.resolve("run-" + ts + backendExtFor(backend));
        try { Files.createDirectories(baseDir); } catch (Throwable ignore) {}

        return RecordingProfile.builder()
                .source(src)
                .fps(fps)
                .width(w)
                .height(h)
                .videoCodec(codec)
                .audioCodec(null) // aktuell kein Audio oder intern geregelt
                .outputFile(out)
                .build();
    }

    private Path resolveOutputDir() {
        SettingsService s = SettingsService.getInstance();
        // Primärer Key (neuer Dialog?)
        String p = s.get("video.outputDir", String.class);
        // Fallback: gleicher Key wie ToggleVideoRecordCommand nutzt
        if (p == null || p.trim().isEmpty()) {
            p = s.get("video.reportsDir", String.class);
        }
        // Weiterer Fallback (optional älterer Name)
        if (p == null || p.trim().isEmpty()) {
            p = s.get("video.baseDir", String.class);
        }
        // Letzter Fallback: Home-Verzeichnis
        if (p == null || p.trim().isEmpty()) {
            p = System.getProperty("user.home") + "/.wd4j/videos";
        }
        return Paths.get(p.trim());
    }

    private static String backendExtFor(String backend) {
        if (backend == null) return ".mp4";
        switch (backend) {
            case "vlc": return ".mp4"; // LibVLC settings mux default
            case "ffmpeg": return ".mkv"; // abhängig von VideoConfig, .mkv als generisch
            case "jcodec": return ".mp4"; // JCodec erzeugt MP4
            default: return ".mp4";
        }
    }

    private static int orInt(Integer v, int d) { return v == null ? d : v; }
    private static String orString(String v, String d) { return (v == null || v.trim().isEmpty()) ? d : v.trim(); }

    private JComponent createClosableTabHeader(final String title,
                                               final JTabbedPane tabs,
                                               final Component tabContent,
                                               final AtomicBoolean running,
                                               final AtomicBoolean recording) {
        Runnable onClose = () -> {
            boolean wasRunning = running.get();
            boolean wasRecording = recording.get();
            if (wasRunning) {
                try { TestPlayerService.getInstance().stopPlayback(); } catch (Throwable ignore) {}
                ApplicationEventBus.getInstance().publish(new StatusMessageEvent("⏹ Playback abgebrochen", 2000));
            }
            // Aufnahme wird im Thread-Finalizer gestoppt (recorder.stop())
            int idx = tabs.indexOfComponent(tabContent);
            if (idx >= 0) tabs.removeTabAt(idx);
        };
        return new de.bund.zrb.ui.tabs.ClosableTabHeader(tabs, tabContent, title, onClose);
    }
}
