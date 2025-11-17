package de.bund.zrb.ui.settings;

import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;

/**
 * Einstellungen für die Status Bar.
 *
 * Aktuell konfigurierbar:
 *  - Maximale Anzahl Einträge in der Verlaufsliste der Statusleiste
 *  - Farben für Hintergrund und Text der Statuszeile
 */
public final class StatusBarSettingsPanel implements SettingsSubPanel {

    private final JPanel root;
    private final JSpinner spHistoryLimit;
    private final JButton btBgColor;
    private final JButton btFgColor;
    private final JButton btResetColors;

    public StatusBarSettingsPanel() {
        root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        GridBagConstraints g = gbc();

        JPanel status = new JPanel(new GridBagLayout());
        status.setBorder(section("Status Bar"));
        GridBagConstraints gs = gbc();

        // Maximale Anzahl Einträge
        JLabel lbLimit = new JLabel("Maximale Anzahl Einträge im Verlauf:");
        gs.gridx = 0; gs.gridy = 0; gs.anchor = GridBagConstraints.WEST; status.add(lbLimit, gs);
        spHistoryLimit = new JSpinner(new SpinnerNumberModel(100, 10, 10_000, 10));
        gs.gridx = 1; gs.gridy = 0; gs.anchor = GridBagConstraints.WEST; status.add(spHistoryLimit, gs);

        // Farben
        btBgColor = new JButton("Hintergrundfarbe wählen...");
        gs.gridx = 0; gs.gridy = 1; gs.gridwidth = 2; gs.anchor = GridBagConstraints.WEST; status.add(btBgColor, gs);

        btFgColor = new JButton("Textfarbe wählen...");
        gs.gridx = 0; gs.gridy = 2; gs.gridwidth = 2; gs.anchor = GridBagConstraints.WEST; status.add(btFgColor, gs);

        btResetColors = new JButton("Auf Standardfarben zurücksetzen");
        gs.gridx = 0; gs.gridy = 3; gs.gridwidth = 2; gs.anchor = GridBagConstraints.WEST; status.add(btResetColors, gs);

        // Listener öffnen den ColorChooser oder setzen zurück
        btBgColor.addActionListener(e -> chooseColor("statusbar.color.bg", btBgColor));
        btFgColor.addActionListener(e -> chooseColor("statusbar.color.fg", btFgColor));
        btResetColors.addActionListener(e -> resetColorsToDefaults());

        g.gridx = 0; g.gridy = 0; g.weightx = 1; g.weighty = 0; g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.NORTHWEST;
        root.add(status, g);

        g.gridx = 0; g.gridy = 1; g.weighty = 1; g.fill = GridBagConstraints.BOTH;
        root.add(Box.createVerticalGlue(), g);
    }

    @Override public String getId() { return "statusbar"; }

    @Override public String getTitle() { return "Status Bar"; }

    @Override public JComponent getComponent() { return root; }

    @Override public void loadFromSettings() {
        Integer limit = SettingsService.getInstance().get("statusbar.history.limit", Integer.class);
        spHistoryLimit.setValue(limit != null && limit > 0 ? limit : 100);
        // Farben aus Settings lesen und als Vorschau in der Button-Hintergrundfarbe anzeigen
        Color bg = getColor("statusbar.color.bg");
        if (bg != null) btBgColor.setBackground(bg);
        else btBgColor.setBackground(UIManager.getColor("Panel.background"));
        Color fg = getColor("statusbar.color.fg");
        if (fg != null) btFgColor.setBackground(fg);
        else btFgColor.setBackground(UIManager.getColor("Panel.background"));

        btBgColor.putClientProperty("statusbar.color.bg", bg);
        btFgColor.putClientProperty("statusbar.color.fg", fg);
    }

    @Override public void putTo(Map<String, Object> out) throws IllegalArgumentException {
        int limit = (Integer) spHistoryLimit.getValue();
        if (limit <= 0) {
            throw new IllegalArgumentException("Die maximale Anzahl Einträge muss größer 0 sein.");
        }
        out.put("statusbar.history.limit", limit);

        // Gewählte Farben aus den ClientProperties holen und speichern (falls vorhanden).
        // Wenn null, wird der Key nicht gesetzt und die StatusBar fällt auf Swing-Default zurück.
        Object bgObj = btBgColor.getClientProperty("statusbar.color.bg");
        if (bgObj instanceof Color) {
            out.put("statusbar.color.bg", ((Color) bgObj).getRGB());
        } else {
            out.put("statusbar.color.bg", null); // explizit zurück auf Default
        }
        Object fgObj = btFgColor.getClientProperty("statusbar.color.fg");
        if (fgObj instanceof Color) {
            out.put("statusbar.color.fg", ((Color) fgObj).getRGB());
        } else {
            out.put("statusbar.color.fg", null);
        }
    }

    private void chooseColor(String key, AbstractButton source) {
        Color initial = (Color) source.getClientProperty(key);
        if (initial == null) {
            initial = source.getBackground();
        }
        Color chosen = JColorChooser.showDialog(root, "Farbe auswählen", initial);
        if (chosen != null) {
            source.putClientProperty(key, chosen);
            source.setBackground(chosen);
        }
    }

    private void resetColorsToDefaults() {
        btBgColor.putClientProperty("statusbar.color.bg", null);
        btFgColor.putClientProperty("statusbar.color.fg", null);
        btBgColor.setBackground(UIManager.getColor("Panel.background"));
        btFgColor.setBackground(UIManager.getColor("Panel.background"));
    }

    private static Color getColor(String key) {
        Integer rgb = SettingsService.getInstance().get(key, Integer.class);
        return rgb != null ? new Color(rgb, true) : null;
    }

    private static GridBagConstraints gbc() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6,8,6,8);
        return gbc;
    }

    private static TitledBorder section(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(title);
        tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD));
        return tb;
    }
}
