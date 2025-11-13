package de.bund.zrb.ui.commands;

import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.video.MediaRecorder;
import de.bund.zrb.video.RecordingProfile;
import de.bund.zrb.video.MediaRuntimeBootstrap;
import de.bund.zrb.video.impl.libvlc.LibVlcLocator;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Toggle command for starting/stopping a window video recording.
 * Prefer LibVLC (local install, no downloads), fall back to FFmpeg/JavaCV if needed.
 * Keep UI free of engine specifics (DIP).
 */
public class ToggleVideoRecordCommand extends ShortcutMenuCommand {

    // Hold recorder for the session (keep simple; avoid static globals in UI code)
    private MediaRecorder recorder;

    @Override
    public String getId() { return "video.toggle"; }

    @Override
    public String getLabel() {
        return "Video-Aufnahme starten/stoppen";
    }

    @Override
    public void perform() {
        try {
            if (recorder != null && recorder.isRecording()) {
                // Stop current recording
                recorder.stop();
                JOptionPane.showMessageDialog(null,
                        "Video-Aufnahme gestoppt.",
                        "Aufnahme", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            // Ensure a recorder is available (LibVLC preferred)
            if (recorder == null) {
                // Strikte Backend-Wahl
                String backend = SettingsService.getInstance().get("video.backend", String.class);
                if (backend == null || backend.trim().isEmpty()) backend = "jcodec";
                backend = backend.trim().toLowerCase(java.util.Locale.ROOT);

                if (backend.equals("vlc")) {
                    // VLC Diagnostik hier (statt Main)
                    System.out.println("vlcj? " + LibVlcLocator.isVlcjAvailable());
                    boolean ok = LibVlcLocator.useVlcjDiscovery() || LibVlcLocator.locateAndConfigure();
                    System.out.println("VLC discovered? " + ok);
                    if (!ok) System.out.println("Hint: Ensure 64-bit VLC and correct VLC_PLUGIN_PATH/jna.library.path");
                }

                try {
                    recorder = MediaRuntimeBootstrap.createRecorder();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null,
                            "Konnte Recorder nicht initialisieren (Backend: " + backend + "):\n" + ex.getMessage(),
                            "Fehler", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            // Build an intent-driven profile (adapt defaults to your needs)
            RecordingProfile profile = createProfileFromSettings();

            // Start recording
            recorder.start(profile);
            JOptionPane.showMessageDialog(null,
                    "Video-Aufnahme gestartet.",
                    "Aufnahme", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Video-Aufnahme konnte nicht umgeschaltet werden:\n" + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Build a sensible default recording profile; adapt to your settings service if present
    private RecordingProfile createProfileFromSettings() {
        // Resolve output folder (~/.wd4j/videos) and timestamped filename
        Path outDir = defaultVideoDir();
        ensureDir(outDir);
        String filename = "capture-" + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date()) + ".mp4";
        Path outFile = outDir.resolve(filename);

        // Use screen capture as generic default; tweak fps/codec as needed
        String source = "screen://";
        // Detect primary screen size for VLC
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = (int) screen.getWidth();
        int height = (int) screen.getHeight();
        int fps = 30;

        // Build via builder (immutable value object)
        // NOTE: If your builder uses different method names (e.g. vCodec/aCodec), adjust accordingly.
        return RecordingProfile.builder()
                .source(source)
                .outputFile(outFile)      // expect Path per current builder example
                .width(width)
                .height(height)
                .fps(fps)
                .videoCodec("h264")
                .audioCodec(null)
                .build();
    }

    private Path defaultVideoDir() {
        String baseDirStr = SettingsService.getInstance().get("video.reportsDir", String.class);
        return Paths.get(baseDirStr);
    }

    private void ensureDir(Path dir) {
        try { Files.createDirectories(dir); } catch (Exception ignore) { /* ignore */ }
    }
}
