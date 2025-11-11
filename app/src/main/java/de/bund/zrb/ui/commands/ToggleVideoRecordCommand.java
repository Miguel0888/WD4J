package de.bund.zrb.ui.commands;

import de.bund.zrb.service.VideoRecordingService;
import de.bund.zrb.service.VideoRuntimeLoader;
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
        return "Video-Aufnahme starten/stoppen";
    }

    @Override
    public void perform() {
        VideoRecordingService svc = VideoRecordingService.getInstance();
        boolean wasRunning = svc.isRecording();

        try {
            if (!wasRunning) {
                // Wenn libs fehlen, erst Benutzer-Dialog anbieten (Download / Manuell / Abbrechen)
                if (!VideoRecordingService.quickCheckAvailable()) {
                    Object[] options = new Object[]{"Download", "Manuell auswählen...", "Abbrechen"};
                    int choice = JOptionPane.showOptionDialog(null,
                            "Die benötigten Video-Bibliotheken fehlen. Wähle eine Option:",
                            "Video-Libs fehlen",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            options,
                            options[0]);

                    if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                        // Abbrechen: wirklich abbrechen, kein weiterer Startversuch
                        return;
                    }

                    boolean ready = false;
                    if (choice == 0) {
                        // Download mit zusätzlicher Bestätigung
                        ready = VideoRuntimeLoader.tryAutoDownloadWithConfirmation(null);
                        if (!ready) {
                            JOptionPane.showMessageDialog(null,
                                    "Download wurde abgebrochen oder ist fehlgeschlagen. Aufnahme wird nicht gestartet.",
                                    "Abgebrochen",
                                    JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                    } else if (choice == 1) {
                        ready = VideoRuntimeLoader.tryManualSelection(null);
                        if (!ready) {
                            JOptionPane.showMessageDialog(null,
                                    "Manuelle Auswahl wurde abgebrochen oder ist fehlerhaft. Aufnahme wird nicht gestartet.",
                                    "Abgebrochen",
                                    JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                    }

                    // Nach Aktion erneut prüfen
                    if (!VideoRecordingService.quickCheckAvailable()) {
                        JOptionPane.showMessageDialog(null,
                                "Video-Stack weiterhin nicht verfügbar. Aufnahme wird nicht gestartet.",
                                "Fehler",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            // Toggle ausführen (start/stop)
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
