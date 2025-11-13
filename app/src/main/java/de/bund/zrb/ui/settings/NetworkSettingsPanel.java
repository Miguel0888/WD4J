package de.bund.zrb.ui.settings;

import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;

/** Panel: Netzwerk */
public final class NetworkSettingsPanel implements SettingsSubPanel {
    private final JPanel root;
    private final JSpinner spWsTimeout, spCmdRetryCount, spCmdRetryWindowS, spNetworkWaitMs;
    private final JCheckBox cbNetworkWaitEnabled;

    public NetworkSettingsPanel() {
        root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        GridBagConstraints g = gbc();

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(section("Netzwerk"));
        GridBagConstraints gb = gbc();
        gb.fill = GridBagConstraints.HORIZONTAL;

        spWsTimeout = new JSpinner(new SpinnerNumberModel(30000.0, 0.0, Double.MAX_VALUE, 100.0));
        gb.gridx=0; gb.gridy=0; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("WebSocket Timeout (ms):"), gb);
        gb.gridx=1; gb.gridy=0; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spWsTimeout, gb);

        spCmdRetryCount = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
        gb.gridx=0; gb.gridy=1; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Command Retry Count:"), gb);
        gb.gridx=1; gb.gridy=1; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spCmdRetryCount, gb);

        spCmdRetryWindowS = new JSpinner(new SpinnerNumberModel(0.0, 0.0, Double.MAX_VALUE, 1.0));
        gb.gridx=0; gb.gridy=2; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Retry-Zeitfenster (s):"), gb);
        gb.gridx=1; gb.gridy=2; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spCmdRetryWindowS, gb);

        cbNetworkWaitEnabled = new JCheckBox("Netzwerk-Wartefunktion aktiv");
        gb.gridx=0; gb.gridy=3; gb.gridwidth=2; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(cbNetworkWaitEnabled, gb); gb.gridwidth=1;

        spNetworkWaitMs = new JSpinner(new SpinnerNumberModel(5000L, 0L, 120_000L, 100L));
        gb.gridx=0; gb.gridy=4; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Timeout (ms):"), gb);
        gb.gridx=1; gb.gridy=4; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spNetworkWaitMs, gb);

        g.gridx=0; g.gridy=0; g.weightx=1; g.fill=GridBagConstraints.HORIZONTAL; root.add(pnl, g);
        g.gridy=1; g.weighty=1; g.fill=GridBagConstraints.BOTH; root.add(Box.createVerticalGlue(), g);
    }

    @Override public String getId() { return "network"; }
    @Override public String getTitle() { return "Netzwerk"; }
    @Override public JComponent getComponent() { return root; }

    @Override public void loadFromSettings() {
        Double wsTimeout = SettingsService.getInstance().get("websocketTimeout", Double.class);
        spWsTimeout.setValue(wsTimeout != null ? wsTimeout : 30000.0);
        Integer cmdRetryCount = SettingsService.getInstance().get("command.retry.maxCount", Integer.class);
        Long cmdRetryWindow = SettingsService.getInstance().get("command.retry.windowMs", Long.class);
        spCmdRetryCount.setValue(cmdRetryCount != null ? cmdRetryCount : 0);
        spCmdRetryWindowS.setValue(cmdRetryWindow != null ? (cmdRetryWindow/1000.0) : 0.0);
        Boolean netWaitEnabled = SettingsService.getInstance().get("network.waitEnabled", Boolean.class);
        cbNetworkWaitEnabled.setSelected(netWaitEnabled == null || netWaitEnabled.booleanValue());
        Long netWaitMs = SettingsService.getInstance().get("network.waitForCompleteMs", Long.class);
        spNetworkWaitMs.setValue(netWaitMs != null && netWaitMs >= 0 ? netWaitMs : 5000L);
    }

    @Override public void putTo(Map<String, Object> out) throws IllegalArgumentException {
        double timeoutValue = ((Number) spWsTimeout.getValue()).doubleValue(); if (timeoutValue < 0) throw new IllegalArgumentException("Timeout darf nicht negativ sein.");
        int cmdRetryCount = ((Number) spCmdRetryCount.getValue()).intValue(); if (cmdRetryCount < 0) throw new IllegalArgumentException("Retry Count darf nicht negativ sein.");
        double cmdRetryWindowS = ((Number) spCmdRetryWindowS.getValue()).doubleValue(); if (cmdRetryWindowS < 0) throw new IllegalArgumentException("Retry-Zeitfenster darf nicht negativ sein.");
        long cmdRetryWindowMs = Math.round(cmdRetryWindowS * 1000.0);
        long netWaitMs = ((Number) spNetworkWaitMs.getValue()).longValue(); if (netWaitMs < 0) throw new IllegalArgumentException("Timeout darf nicht negativ sein.");

        out.put("websocketTimeout", timeoutValue);
        out.put("command.retry.maxCount", cmdRetryCount);
        out.put("command.retry.windowMs", cmdRetryWindowMs);
        out.put("network.waitEnabled", cbNetworkWaitEnabled.isSelected());
        out.put("network.waitForCompleteMs", netWaitMs);
    }

    private static GridBagConstraints gbc(){ GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(6,8,6,8); return gbc; }
    private static TitledBorder section(String t){ TitledBorder tb=BorderFactory.createTitledBorder(t); tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD)); return tb; }
}
