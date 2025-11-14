package de.bund.zrb.ui.commands;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.StatusMessageEvent;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.video.overlay.VideoOverlayService;
import de.bund.zrb.video.overlay.VideoOverlayStyle;

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
                VideoOverlayService svc = VideoOverlayService.getInstance();
                JDialog dlg = new JDialog((Frame) null, "Overlay-Einstellungen", true);
                dlg.setLayout(new BorderLayout(10,10));

                JTabbedPane tabs = new JTabbedPane();

                // Allgemein-Panel
                JPanel general = new JPanel();
                general.setLayout(new BoxLayout(general, BoxLayout.Y_AXIS));
                JCheckBox cbEnabled = new JCheckBox("Overlay aktivieren", svc.isEnabled());
                cbEnabled.addActionListener(e -> svc.setEnabled(cbEnabled.isSelected()));
                general.add(cbEnabled);
                general.add(Box.createVerticalStrut(8));
                JLabel help = new JLabel("Konfiguration der Anzeige im aufgenommenen Video.");
                help.setAlignmentX(0f);
                general.add(help);
                tabs.addTab("Allgemein", wrapScroll(general));

                // Caption (Suite/Root)
                tabs.addTab("Caption", buildStylePanel(
                        "Caption (Suite/Root)",
                        svc.isCaptionEnabled(),
                        b -> svc.setCaptionEnabled(b),
                        svc.getCaptionStyle(),
                        s -> svc.applyCaptionStyle(s)
                ));

                // Subtitle (Case)
                tabs.addTab("Subtitle", buildStylePanel(
                        "Subtitle (Case)",
                        svc.isSubtitleEnabled(),
                        b -> svc.setSubtitleEnabled(b),
                        svc.getSubtitleStyle(),
                        s -> svc.applySubtitleStyle(s)
                ));

                // Action transient
                JPanel actionPanel = new JPanel();
                actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
                JCheckBox cbAction = new JCheckBox("Transient Action Overlay aktivieren", svc.isActionTransientEnabled());
                cbAction.addActionListener(e -> svc.setActionTransientEnabled(cbAction.isSelected()));
                actionPanel.add(cbAction);
                actionPanel.add(Box.createVerticalStrut(6));
                VideoOverlayStyle actStyle = svc.getActionStyle();
                JTextField tfFontColor = new JTextField(actStyle.getFontColor());
                JTextField tfBgColor = new JTextField(actStyle.getBackgroundColor());
                JSpinner spFontSize = new JSpinner(new SpinnerNumberModel(actStyle.getFontSizePx(), 8, 96, 1));
                JSpinner spDuration = new JSpinner(new SpinnerNumberModel(svc.getActionTransientDurationMs(), 200, 10000, 100));
                addLabeled(actionPanel, "Font Color", tfFontColor);
                addLabeled(actionPanel, "Background Color", tfBgColor);
                addLabeled(actionPanel, "Font Size (px)", spFontSize);
                addLabeled(actionPanel, "Dauer (ms)", spDuration);
                JButton btnPreview = new JButton("Preview");
                btnPreview.addActionListener(e -> {
                    svc.applyActionStyle(new VideoOverlayStyle(tfFontColor.getText().trim(), tfBgColor.getText().trim(), (Integer) spFontSize.getValue()));
                    svc.setActionTransientDurationMs(((Number) spDuration.getValue()).intValue());
                    if (cbAction.isSelected()) {
                        de.bund.zrb.event.ApplicationEventBus.getInstance().publish(new de.bund.zrb.video.overlay.VideoOverlayEvent(de.bund.zrb.video.overlay.VideoOverlayEvent.Kind.ACTION, "Action Beispiel"));
                    }
                });
                actionPanel.add(Box.createVerticalStrut(6));
                actionPanel.add(btnPreview);
                tabs.addTab("Action", wrapScroll(actionPanel));

                dlg.add(tabs, BorderLayout.CENTER);

                JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                JButton close = new JButton("Schließen");
                close.addActionListener(e -> dlg.dispose());
                south.add(close);
                dlg.add(south, BorderLayout.SOUTH);

                dlg.setMinimumSize(new Dimension(520, 480));
                dlg.setLocationRelativeTo(null);
                dlg.pack();
                dlg.setVisible(true);
            } catch (Exception ex) {
                ApplicationEventBus.getInstance().publish(new StatusMessageEvent(
                        "Overlay-Dialog konnte nicht geöffnet werden: " + ex.getMessage(), 4000));
            }
        });
    }

    private JScrollPane buildStylePanel(String title,
                                        boolean enabled,
                                        java.util.function.Consumer<Boolean> enableConsumer,
                                        VideoOverlayStyle style,
                                        java.util.function.Consumer<VideoOverlayStyle> styleConsumer) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JCheckBox cb = new JCheckBox(title + " aktiv", enabled);
        cb.addActionListener(e -> enableConsumer.accept(cb.isSelected()));
        panel.add(cb);
        panel.add(Box.createVerticalStrut(6));
        JTextField tfFontColor = new JTextField(style.getFontColor());
        JTextField tfBgColor = new JTextField(style.getBackgroundColor());
        JSpinner spFontSize = new JSpinner(new SpinnerNumberModel(style.getFontSizePx(), 8, 96, 1));
        addLabeled(panel, "Font Color", tfFontColor);
        addLabeled(panel, "Background Color", tfBgColor);
        addLabeled(panel, "Font Size (px)", spFontSize);
        JButton preview = new JButton("Preview");
        preview.addActionListener(e -> {
            VideoOverlayStyle s = new VideoOverlayStyle(tfFontColor.getText().trim(), tfBgColor.getText().trim(), (Integer) spFontSize.getValue());
            styleConsumer.accept(s);
            if (cb.isSelected()) {
                // Test-Event auslösen
                de.bund.zrb.event.ApplicationEventBus.getInstance().publish(new de.bund.zrb.video.overlay.VideoOverlayEvent(
                        title.startsWith("Caption") ? de.bund.zrb.video.overlay.VideoOverlayEvent.Kind.SUITE : de.bund.zrb.video.overlay.VideoOverlayEvent.Kind.CASE,
                        "Beispiel"));
            }
        });
        panel.add(Box.createVerticalStrut(6));
        panel.add(preview);
        return wrapScroll(panel);
    }

    private static void addLabeled(JPanel p, String label, JComponent comp) {
        JPanel row = new JPanel(new BorderLayout(6,0));
        JLabel l = new JLabel(label + ":");
        row.add(l, BorderLayout.WEST);
        row.add(comp, BorderLayout.CENTER);
        row.setAlignmentX(0f);
        p.add(row);
        p.add(Box.createVerticalStrut(4));
    }

    private static JScrollPane wrapScroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createEmptyBorder());
        return sp;
    }
}
