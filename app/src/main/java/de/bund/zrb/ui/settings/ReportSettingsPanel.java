package de.bund.zrb.ui.settings;

import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;

/** Panel: Bericht/Report */
public final class ReportSettingsPanel implements SettingsSubPanel {
    private final JPanel root;
    private final JTextField tfReportDir;

    public ReportSettingsPanel() {
        root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        GridBagConstraints g = gbc();

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(section("Bericht"));
        GridBagConstraints gb = gbc();

        gb.gridx=0; gb.gridy=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Report-Verzeichnis:"), gb);
        tfReportDir = new JTextField(28);
        gb.gridx=1; gb.gridy=0; gb.anchor=GridBagConstraints.EAST; pnl.add(tfReportDir, gb);
        JButton btBrowse = new JButton("Durchsuchen…");
        btBrowse.setFocusable(false);
        btBrowse.addActionListener(e -> chooseDirInto(tfReportDir));
        gb.gridx=1; gb.gridy=1; gb.anchor=GridBagConstraints.EAST; pnl.add(btBrowse, gb);

        g.gridx=0; g.gridy=0; g.weightx=1; g.fill=GridBagConstraints.HORIZONTAL; root.add(pnl, g);
        g.gridy=1; g.weighty=1; g.fill=GridBagConstraints.BOTH; root.add(Box.createVerticalGlue(), g);
    }

    @Override public String getId() { return "report"; }
    @Override public String getTitle() { return "Bericht"; }
    @Override public JComponent getComponent() { return root; }

    @Override public void loadFromSettings() {
        String reportDir = SettingsService.getInstance().get("reportBaseDir", String.class);
        tfReportDir.setText((reportDir != null && !reportDir.trim().isEmpty()) ? reportDir : "C:/Reports");
    }

    @Override public void putTo(Map<String, Object> out) throws IllegalArgumentException {
        String reportDir = tfReportDir.getText().trim();
        if (reportDir.isEmpty()) throw new IllegalArgumentException("Bitte ein Report-Verzeichnis angeben.");
        out.put("reportBaseDir", reportDir);
    }

    private static GridBagConstraints gbc(){ GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(6,8,6,8); return gbc; }
    private static TitledBorder section(String t){ TitledBorder tb=BorderFactory.createTitledBorder(t); tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD)); return tb; }
    private void chooseDirInto(JTextField target){ JFileChooser ch=new JFileChooser(); ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); if(ch.showOpenDialog(root)==JFileChooser.APPROVE_OPTION){ target.setText(ch.getSelectedFile().getAbsolutePath()); }}
}

