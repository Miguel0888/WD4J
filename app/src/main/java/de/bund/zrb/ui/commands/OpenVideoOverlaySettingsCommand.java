package de.bund.zrb.ui.commands;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.StatusMessageEvent;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.video.overlay.VideoOverlayService;

import javax.swing.*;
import java.awt.*;

/** Öffnet einen kleinen Dialog mit Overlay-Einstellungen (inkl. Checkbox zum Aktivieren). */
public class OpenVideoOverlaySettingsCommand extends ShortcutMenuCommand {

    @Override public String getId() { return "video.overlay"; }
    @Override public String getLabel() { return "Overlay-Einstellungen"; }

    @Override
    public void perform() {
        SwingUtilities.invokeLater(() -> {
            try {
                JDialog dlg = new JDialog((Frame) null, "Overlay-Einstellungen", true);
                dlg.setLayout(new BorderLayout(8,8));

                JPanel form = new JPanel();
                form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

                JCheckBox cbEnable = new JCheckBox("Overlay anzeigen");
                cbEnable.setSelected(VideoOverlayService.getInstance().isEnabled());
                cbEnable.addActionListener(e -> VideoOverlayService.getInstance().setEnabled(cbEnable.isSelected()));
                form.add(cbEnable);

                // Platzhalter: Hier könnten Stil-Optionen ergänzt werden (Farben, Größe, Transparenz)
                form.add(Box.createVerticalStrut(8));
                form.add(new JLabel("Weitere Optionen folgen (Farbe, Schriftgröße, Transparenz)..."));

                JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton close = new JButton("Schließen");
                close.addActionListener(e -> dlg.dispose());
                south.add(close);

                dlg.add(form, BorderLayout.CENTER);
                dlg.add(south, BorderLayout.SOUTH);
                dlg.getRootPane().setDefaultButton(close);
                dlg.pack();
                dlg.setLocationRelativeTo(null);
                dlg.setVisible(true);
            } catch (Exception ex) {
                ApplicationEventBus.getInstance().publish(new StatusMessageEvent(
                        "Overlay-Dialog konnte nicht geöffnet werden: " + ex.getMessage(), 4000));
            }
        });
    }
}

