package de.bund.zrb.ui.settings;

import de.bund.zrb.config.VideoConfig;
import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;

/** Panel: Aufnahme/Video */
public final class RecordingSettingsPanel implements SettingsSubPanel {
    private final JPanel root;
    private final JCheckBox cbVideoEnabled;
    private final JSpinner spVideoFps;
    private final JTextField tfVideoDir;

    public RecordingSettingsPanel() {
        root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        GridBagConstraints g = gbc();

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(section("Aufnahme"));
        GridBagConstraints gb = gbc();

        cbVideoEnabled = new JCheckBox("Fenster-Video aufzeichnen (Windows)");
        gb.gridx=0; gb.gridy=0; gb.gridwidth=2; gb.anchor=GridBagConstraints.WEST; pnl.add(cbVideoEnabled, gb); gb.gridwidth=1;

        gb.gridx=0; gb.gridy=1; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("FPS:"), gb);
        spVideoFps = new JSpinner(new SpinnerNumberModel(15, 1, 120, 1));
        gb.gridx=1; gb.gridy=1; gb.anchor=GridBagConstraints.EAST; pnl.add(spVideoFps, gb);

        gb.gridx=0; gb.gridy=2; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Video-Ordner:"), gb);
        tfVideoDir = new JTextField(28);
        gb.gridx=1; gb.gridy=2; gb.anchor=GridBagConstraints.EAST; pnl.add(tfVideoDir, gb);
        JButton btBrowse = new JButton("Durchsuchen…");
        btBrowse.setFocusable(false);
        btBrowse.addActionListener(e -> chooseDirInto(tfVideoDir));
        gb.gridx=1; gb.gridy=3; gb.anchor=GridBagConstraints.EAST; pnl.add(btBrowse, gb);

        g.gridx=0; g.gridy=0; g.weightx=1; g.fill=GridBagConstraints.HORIZONTAL; root.add(pnl, g);
        g.gridy=1; g.weighty=1; g.fill=GridBagConstraints.BOTH; root.add(Box.createVerticalGlue(), g);
    }

    @Override public String getId() { return "recording"; }
    @Override public String getTitle() { return "Aufnahme"; }
    @Override public JComponent getComponent() { return root; }

    @Override public void loadFromSettings() {
        Boolean videoEnabled = SettingsService.getInstance().get("video.enabled", Boolean.class);
        Integer videoFps     = SettingsService.getInstance().get("video.fps", Integer.class);
        String  videoDir     = SettingsService.getInstance().get("video.reportsDir", String.class);
        cbVideoEnabled.setSelected(videoEnabled != null ? videoEnabled : false);
        spVideoFps.setValue(videoFps != null ? videoFps : 15);
        if (videoDir != null && !videoDir.trim().isEmpty()) tfVideoDir.setText(videoDir);
        else tfVideoDir.setText(VideoConfig.getReportsDir());
    }

    @Override public void putTo(Map<String, Object> out) throws IllegalArgumentException {
        int fps = ((Number) spVideoFps.getValue()).intValue();
        if (fps <= 0) throw new IllegalArgumentException("FPS muss > 0 sein.");
        String videoDir = tfVideoDir.getText().trim();
        if (videoDir.isEmpty()) throw new IllegalArgumentException("Bitte einen Video-Ordner angeben.");
        out.put("video.enabled", cbVideoEnabled.isSelected());
        out.put("video.fps", fps);
        out.put("video.reportsDir", videoDir);
    }

    private static GridBagConstraints gbc() { GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(6,8,6,8); return gbc; }
    private static TitledBorder section(String title){ TitledBorder tb=BorderFactory.createTitledBorder(title); tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD)); return tb; }
    private void chooseDirInto(JTextField target){ JFileChooser ch=new JFileChooser(); ch.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); if(ch.showOpenDialog(root)==JFileChooser.APPROVE_OPTION){ target.setText(ch.getSelectedFile().getAbsolutePath()); }}
}

