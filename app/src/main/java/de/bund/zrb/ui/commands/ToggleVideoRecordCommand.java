package de.bund.zrb.ui.commands;

import de.bund.zrb.service.VideoRecordingService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;

import javax.swing.*;

/**
 * Ein Command, das die Fenster-Videoaufnahme toggelt (Start/Stop).
 * Unabhängig von Auto-Start-Einstellungen. Nutzt VideoConfig (fps/Ordner).
 */
public class ToggleVideoRecordCommand extends ShortcutMenuCommand {

    @Override
    public String getId() { return "video.toggle"; }

    @Override
    public String getLabel() {
        return VideoRecordingService.getInstance().isRecording()
                ? "Video-Aufnahme starten/stoppen"
                : "Video-Aufnahme starten/stoppen";
    }

    @Override
    public void perform() {
        try {
            VideoRecordingService svc = VideoRecordingService.getInstance();
            boolean wasRunning = svc.isRecording();
            svc.toggle();

            if (wasRunning) {
                JOptionPane.showMessageDialog(null, "Video-Aufnahme gestoppt.",
                        "Aufnahme", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(null, "Video-Aufnahme gestartet.",
                        "Aufnahme", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Video-Aufnahme konnte nicht umgeschaltet werden:\n" + ex.getMessage(),
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }
}
