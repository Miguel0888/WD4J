package de.bund.zrb.ui.settings;

import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;

/** Panel "Allgemein" – enthält UI-Optionen und ggf. globale Kleinigkeiten. */
public final class GeneralSettingsPanel implements SettingsSubPanel {

    private final JPanel root;
    private final JCheckBox cbHideHelpButtons;
    private final JCheckBox cbShowPhases; // neu

    public GeneralSettingsPanel() {
        root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        GridBagConstraints g = gbc();

        JPanel ui = new JPanel(new GridBagLayout());
        ui.setBorder(section("UI"));
        GridBagConstraints gu = gbc();
        cbHideHelpButtons = new JCheckBox("Help-Buttons ausblenden");
        gu.gridx = 0; gu.gridy = 0; gu.anchor = GridBagConstraints.WEST; ui.add(cbHideHelpButtons, gu);
        cbShowPhases = new JCheckBox("Given/When/Then anzeigen");
        gu.gridy = 1; ui.add(cbShowPhases, gu);

        g.gridx = 0; g.gridy = 0; g.anchor = GridBagConstraints.NORTHWEST; g.weightx = 1; g.weighty = 0; g.fill = GridBagConstraints.HORIZONTAL;
        root.add(ui, g);

        g.gridx = 0; g.gridy = 1; g.weighty = 1; g.fill = GridBagConstraints.BOTH;
        root.add(Box.createVerticalGlue(), g);
    }

    @Override public String getId() { return "general"; }
    @Override public String getTitle() { return "Allgemein"; }
    @Override public JComponent getComponent() { return root; }

    @Override public void loadFromSettings() {
        Boolean hide = SettingsService.getInstance().get("ui.helpButtons.hide", Boolean.class);
        cbHideHelpButtons.setSelected(hide != null && hide);
        Boolean phases = SettingsService.getInstance().get("logging.phase.enabled", Boolean.class);
        cbShowPhases.setSelected(phases == null || phases.booleanValue()); // default an
    }

    @Override public void putTo(Map<String, Object> out) throws IllegalArgumentException {
        out.put("ui.helpButtons.hide", cbHideHelpButtons.isSelected());
        out.put("logging.phase.enabled", cbShowPhases.isSelected());
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
