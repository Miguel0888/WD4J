package de.bund.zrb.ui.commands;

import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.video.MediaRecorder;
import de.bund.zrb.video.RecordingProfile;
import de.bund.zrb.video.MediaRuntimeBootstrap;
import de.bund.zrb.service.VideoRuntimeLoader;
import de.bund.zrb.service.VideoRecordingService; // nur für quickCheckAvailable() als Fallback-Check

import javax.swing.*;
import java.awt.*;
import java.io.File;
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
                recorder = tryInitLibVlcFirstOrFallbackToFfmpegInteractive(null);
                if (recorder == null) {
                    // User cancelled or environment not ready
                    JOptionPane.showMessageDialog(null,
                            "Video-Stack nicht verfügbar. Aufnahme wird nicht gestartet.",
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

    // Try LibVLC first; if unavailable, interactively prepare FFmpeg and use adapter
    private MediaRecorder tryInitLibVlcFirstOrFallbackToFfmpegInteractive(Component parent) {
        // 1) Try LibVLC (local VLC install via vlcj); no downloads
        MediaRecorder libvlc = MediaRuntimeBootstrap.tryCreateLibVlcRecorder();
        if (libvlc != null) {
            return libvlc;
        }

        // 2) FFmpeg path: if missing, ask user (download/manual/cancel) like legacy flow
        if (!VideoRecordingService.quickCheckAvailable()) {
            Object[] options = new Object[] { "Download", "Manuell auswählen...", "Abbrechen" };
            int choice = JOptionPane.showOptionDialog(parent,
                    "Die benötigten Video-Bibliotheken fehlen. Wähle eine Option:",
                    "Video-Libs fehlen",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                return null; // user cancelled
            }
            boolean ready = false;
            if (choice == 0) {
                ready = VideoRuntimeLoader.tryAutoDownloadWithConfirmation(parent);
                if (!ready) {
                    JOptionPane.showMessageDialog(parent,
                            "Download wurde abgebrochen oder ist fehlgeschlagen.",
                            "Abgebrochen", JOptionPane.INFORMATION_MESSAGE);
                    return null;
                }
            } else if (choice == 1) {
                ready = VideoRuntimeLoader.tryManualSelection(parent);
                if (!ready) {
                    JOptionPane.showMessageDialog(parent,
                            "Manuelle Auswahl wurde abgebrochen oder ist fehlerhaft.",
                            "Abgebrochen", JOptionPane.INFORMATION_MESSAGE);
                    return null;
                }
            }

            // Re-check availability after user action
            if (!VideoRecordingService.quickCheckAvailable()) {
                JOptionPane.showMessageDialog(parent,
                        "Video-Stack weiterhin nicht verfügbar.",
                        "Fehler", JOptionPane.ERROR_MESSAGE);
                return null;
            }
        }

        // 3) Create FFmpeg adapter (no UI knowledge about FFmpeg internals here)
        return MediaRuntimeBootstrap.createFfmpegRecorder();
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
//        int width = 0;   // keep source resolution
//        int height = 0;  // keep source resolution
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
                .audioCodec("mp4a")
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
