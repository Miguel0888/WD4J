package de.bund.zrb.ui.settings;

import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;

/** Panel: Browser */
public final class BrowserSettingsPanel implements SettingsSubPanel {

    private final JPanel root;
    private final JComboBox<String> cbBrowserType;
    private final JSpinner spPort;
    private final JCheckBox cbHeadless, cbDisableGpu, cbNoRemote, cbStartMax, cbUseProfile, cbConfirmTerminateRunning;
    private final JTextField tfProfilePath, tfExtraArgs;

    public BrowserSettingsPanel() {
        root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        GridBagConstraints g = gbc();

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(section("Browser"));
        GridBagConstraints gb = gbc();

        cbBrowserType = new JComboBox<String>(new String[]{"firefox","chromium","edge"});
        gb.gridx=0; gb.gridy=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Browser:"), gb);
        gb.gridx=1; gb.gridy=0; gb.anchor=GridBagConstraints.EAST; pnl.add(cbBrowserType, gb);

        spPort = new JSpinner(new SpinnerNumberModel(9222, 0, 65535, 1));
        gb.gridx=0; gb.gridy=1; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Port:"), gb);
        gb.gridx=1; gb.gridy=1; gb.anchor=GridBagConstraints.EAST; pnl.add(spPort, gb);

        cbHeadless = new JCheckBox("Headless");
        cbDisableGpu = new JCheckBox("GPU deaktivieren");
        cbNoRemote = new JCheckBox("No-Remote");
        cbStartMax = new JCheckBox("Maximiert starten");
        cbUseProfile = new JCheckBox("Profil verwenden");
        cbConfirmTerminateRunning = new JCheckBox("Vor Start laufende Browser-Instanzen beenden (nach Rückfrage)");

        gb.gridx=0; gb.gridy=2; gb.gridwidth=2; gb.anchor=GridBagConstraints.WEST; pnl.add(cbHeadless, gb);
        gb.gridy=3; pnl.add(cbDisableGpu, gb);
        gb.gridy=4; pnl.add(cbNoRemote, gb);
        gb.gridy=5; pnl.add(cbStartMax, gb);
        gb.gridy=6; pnl.add(cbUseProfile, gb);
        gb.gridy=7; pnl.add(cbConfirmTerminateRunning, gb);
        gb.gridwidth=1;

        tfProfilePath = new JTextField(24);
        tfExtraArgs = new JTextField(28);
        JButton btBrowseProfile = new JButton("Durchsuchen…");
        btBrowseProfile.setFocusable(false);
        btBrowseProfile.addActionListener(e -> chooseDirInto(tfProfilePath));

        gb.gridx=0; gb.gridy=8; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Profilpfad:"), gb);
        gb.gridx=1; gb.gridy=8; gb.anchor=GridBagConstraints.EAST; pnl.add(tfProfilePath, gb);
        gb.gridx=1; gb.gridy=9; gb.anchor=GridBagConstraints.EAST; pnl.add(btBrowseProfile, gb);

        gb.gridx=0; gb.gridy=10; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Extra-Args:"), gb);
        gb.gridx=1; gb.gridy=10; gb.anchor=GridBagConstraints.EAST; pnl.add(tfExtraArgs, gb);

        g.gridx=0; g.gridy=0; g.weightx=1; g.fill=GridBagConstraints.HORIZONTAL;
        root.add(pnl, g);
        g.gridy=1; g.weighty=1; g.fill=GridBagConstraints.BOTH; root.add(Box.createVerticalGlue(), g);
    }

    @Override public String getId() { return "browser"; }
    @Override public String getTitle() { return "Browser"; }
    @Override public JComponent getComponent() { return root; }

    @Override public void loadFromSettings() {
        String sel = SettingsService.getInstance().get("browser.selected", String.class);
        cbBrowserType.setSelectedItem(sel != null ? sel.toLowerCase() : "firefox");
        Integer port = SettingsService.getInstance().get("browser.port", Integer.class);
        spPort.setValue(port != null ? port : 9222);
        cbHeadless.setSelected(Boolean.TRUE.equals(SettingsService.getInstance().get("browser.headless", Boolean.class)));
        cbDisableGpu.setSelected(Boolean.TRUE.equals(SettingsService.getInstance().get("browser.disableGpu", Boolean.class)));
        cbNoRemote.setSelected(Boolean.TRUE.equals(SettingsService.getInstance().get("browser.noRemote", Boolean.class)));
        Boolean sm = SettingsService.getInstance().get("browser.startMaximized", Boolean.class);
        cbStartMax.setSelected(sm != null ? sm : true);
        cbUseProfile.setSelected(Boolean.TRUE.equals(SettingsService.getInstance().get("browser.useProfile", Boolean.class)));
        String path = SettingsService.getInstance().get("browser.profilePath", String.class);
        tfProfilePath.setText(path != null ? path : "");
        String args = SettingsService.getInstance().get("browser.extraArgs", String.class);
        tfExtraArgs.setText(args != null ? args : "");
        Boolean c = SettingsService.getInstance().get("browser.confirmTerminateRunning", Boolean.class);
        cbConfirmTerminateRunning.setSelected(c != null ? c : true);
    }

    @Override public void putTo(Map<String, Object> out) throws IllegalArgumentException {
        out.put("browser.selected", String.valueOf(cbBrowserType.getSelectedItem()).toLowerCase());
        out.put("browser.port", ((Number) spPort.getValue()).intValue());
        out.put("browser.headless", cbHeadless.isSelected());
        out.put("browser.disableGpu", cbDisableGpu.isSelected());
        out.put("browser.noRemote", cbNoRemote.isSelected());
        out.put("browser.startMaximized", cbStartMax.isSelected());
        out.put("browser.useProfile", cbUseProfile.isSelected());
        out.put("browser.profilePath", tfProfilePath.getText().trim());
        out.put("browser.extraArgs", tfExtraArgs.getText().trim());
        out.put("browser.confirmTerminateRunning", cbConfirmTerminateRunning.isSelected());
    }

    private static GridBagConstraints gbc() { GridBagConstraints gbc = new GridBagConstraints(); gbc.insets=new Insets(6,8,6,8); return gbc; }
    private static TitledBorder section(String title){ TitledBorder tb=BorderFactory.createTitledBorder(title); tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD)); return tb; }

    private void chooseDirInto(JTextField target) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int res = chooser.showOpenDialog(root);
        if (res == JFileChooser.APPROVE_OPTION) {
            target.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }
}

