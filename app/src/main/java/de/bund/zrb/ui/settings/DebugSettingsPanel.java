package de.bund.zrb.ui.settings;

import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;

/** Panel: Debug / Terminal */
public final class DebugSettingsPanel implements SettingsSubPanel {
    private final JPanel root;
    private final JCheckBox cbDebugEnabled, cbLogWebSocket, cbLogVideo, cbLogBrowser, cbLogTabManager, cbLogNetwork;

    public DebugSettingsPanel() {
        root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        GridBagConstraints g = gbc();

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(section("Debug / Terminal"));
        GridBagConstraints gd = gbc();

        cbDebugEnabled = new JCheckBox("[Debug]-Logs");
        cbLogWebSocket = new JCheckBox("[WebSocket]-Logs");
        cbLogVideo = new JCheckBox("[Video]-Logs");
        cbLogBrowser = new JCheckBox("[Browser]-Logs");
        cbLogTabManager = new JCheckBox("[TabManager]-Logs");
        cbLogNetwork = new JCheckBox("[Playback]-Logs");

        int row=0;
        gd.gridx=0; gd.gridy=row; gd.anchor=GridBagConstraints.WEST; pnl.add(cbDebugEnabled, gd);
        gd.gridx=1; gd.gridy=row; gd.anchor=GridBagConstraints.WEST; pnl.add(cbLogWebSocket, gd);
        gd.gridx=2; gd.gridy=row++; gd.anchor=GridBagConstraints.WEST; pnl.add(cbLogVideo, gd);

        gd.gridx=0; gd.gridy=row; gd.anchor=GridBagConstraints.WEST; pnl.add(cbLogBrowser, gd);
        gd.gridx=1; gd.gridy=row; gd.anchor=GridBagConstraints.WEST; pnl.add(cbLogTabManager, gd);
        gd.gridx=2; gd.gridy=row++; gd.anchor=GridBagConstraints.WEST; pnl.add(cbLogNetwork, gd);

        // Optional: kleiner Hinweis, dass Browser-Text dynamisch mit Browser-Auswahl variiert
        JLabel hint = new JLabel("Hinweis: Text in [Browser]-Logs hängt von der Browser-Auswahl ab.");
        hint.setForeground(new Color(0x666666));
        gd.gridx=0; gd.gridy=row; gd.gridwidth=3; gd.anchor=GridBagConstraints.WEST; pnl.add(hint, gd); gd.gridwidth=1;

        g.gridx=0; g.gridy=0; g.weightx=1; g.fill=GridBagConstraints.HORIZONTAL; root.add(pnl, g);
        g.gridy=1; g.weighty=1; g.fill=GridBagConstraints.BOTH; root.add(Box.createVerticalGlue(), g);
    }

    @Override public String getId() { return "debug"; }
    @Override public String getTitle() { return "Debug"; }
    @Override public JComponent getComponent() { return root; }

    @Override public void loadFromSettings() {
        Boolean dbgEnabled = SettingsService.getInstance().get("debug.enabled", Boolean.class);
        Boolean dbgWs      = SettingsService.getInstance().get("debug.websocket", Boolean.class);
        Boolean dbgVid     = SettingsService.getInstance().get("debug.video", Boolean.class);
        Boolean dbgBrowser = SettingsService.getInstance().get("debug.browser", Boolean.class);
        Boolean dbgTab     = SettingsService.getInstance().get("debug.tabmanager", Boolean.class);
        Boolean dbgNet     = SettingsService.getInstance().get("debug.network", Boolean.class);
        cbDebugEnabled.setSelected(dbgEnabled != null && dbgEnabled);
        cbLogWebSocket.setSelected(dbgWs != null && dbgWs);
        cbLogVideo.setSelected(dbgVid != null && dbgVid);
        cbLogBrowser.setSelected(dbgBrowser != null && dbgBrowser);
        cbLogTabManager.setSelected(dbgTab != null && dbgTab);
        cbLogNetwork.setSelected(dbgNet != null && dbgNet);
    }

    @Override public void putTo(Map<String, Object> out) throws IllegalArgumentException {
        out.put("debug.enabled", cbDebugEnabled.isSelected());
        out.put("debug.websocket", cbLogWebSocket.isSelected());
        out.put("debug.video", cbLogVideo.isSelected());
        out.put("debug.browser", cbLogBrowser.isSelected());
        out.put("debug.tabmanager", cbLogTabManager.isSelected());
        out.put("debug.network", cbLogNetwork.isSelected());

        System.setProperty("wd4j.debug", String.valueOf(cbDebugEnabled.isSelected()));
        System.setProperty("wd4j.log.websocket", String.valueOf(cbLogWebSocket.isSelected()));
        System.setProperty("wd4j.log.video", String.valueOf(cbLogVideo.isSelected()));
        System.setProperty("wd4j.log.browser", String.valueOf(cbLogBrowser.isSelected()));
        System.setProperty("wd4j.log.tabmanager", String.valueOf(cbLogTabManager.isSelected()));
        System.setProperty("wd4j.log.network", String.valueOf(cbLogNetwork.isSelected()));
    }

    private static GridBagConstraints gbc(){ GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(6,8,6,8); return gbc; }
    private static TitledBorder section(String t){ TitledBorder tb=BorderFactory.createTitledBorder(t); tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD)); return tb; }
}
