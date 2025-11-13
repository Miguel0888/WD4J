package de.bund.zrb.ui.settings;

import de.bund.zrb.config.InputDelaysConfig;
import de.bund.zrb.service.SettingsService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/** Panel: Wiedergabe/Eingabe */
public final class InputSettingsPanel implements SettingsSubPanel {
    private final JPanel root;
    private final JSpinner spKeyDown, spKeyUp, spActionDefaultTimeoutMs, spAssertGroupWait, spAssertEachWait, spBeforeEachAfterWait;

    public InputSettingsPanel() {
        root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        GridBagConstraints g = gbc();

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(section("Wiedergabe"));
        GridBagConstraints gb = gbc();

        spKeyDown = new JSpinner(new SpinnerNumberModel(10, 0, 2000, 1));
        spKeyUp   = new JSpinner(new SpinnerNumberModel(30, 0, 2000, 1));
        spActionDefaultTimeoutMs = new JSpinner(new SpinnerNumberModel(30000, 0, 3_600_000, 100));
        spAssertGroupWait = new JSpinner(new SpinnerNumberModel(3.0, 0.0, Double.MAX_VALUE, 1.0));
        spAssertEachWait  = new JSpinner(new SpinnerNumberModel(0.0, 0.0, Double.MAX_VALUE, 1.0));
        spBeforeEachAfterWait = new JSpinner(new SpinnerNumberModel(0.0, 0.0, Double.MAX_VALUE, 1.0));

        gb.gridx=0; gb.gridy=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("KeyDown-Delay (ms):"), gb);
        gb.gridx=1; gb.gridy=0; gb.anchor=GridBagConstraints.EAST; pnl.add(spKeyDown, gb);
        gb.gridx=0; gb.gridy=1; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("KeyUp-Delay (ms):"), gb);
        gb.gridx=1; gb.gridy=1; gb.anchor=GridBagConstraints.EAST; pnl.add(spKeyUp, gb);

        gb.gridx=0; gb.gridy=2; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Default Action-Timeout (ms):"), gb);
        gb.gridx=1; gb.gridy=2; gb.anchor=GridBagConstraints.EAST; pnl.add(spActionDefaultTimeoutMs, gb);

        gb.gridx=0; gb.gridy=3; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Wartezeit vor Assertions (s):"), gb);
        gb.gridx=1; gb.gridy=3; gb.anchor=GridBagConstraints.EAST; pnl.add(spAssertGroupWait, gb);
        gb.gridx=0; gb.gridy=4; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Wartezeit zwischen Assertions (s):"), gb);
        gb.gridx=1; gb.gridy=4; gb.anchor=GridBagConstraints.EAST; pnl.add(spAssertEachWait, gb);
        gb.gridx=0; gb.gridy=5; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Wartezeit nach BeforeEach (s):"), gb);
        gb.gridx=1; gb.gridy=5; gb.anchor=GridBagConstraints.EAST; pnl.add(spBeforeEachAfterWait, gb);

        g.gridx=0; g.gridy=0; g.weightx=1; g.fill=GridBagConstraints.HORIZONTAL; root.add(pnl, g);
        g.gridy=1; g.weighty=1; g.fill=GridBagConstraints.BOTH; root.add(Box.createVerticalGlue(), g);
    }

    @Override public String getId() { return "input"; }
    @Override public String getTitle() { return "Wiedergabe"; }
    @Override public JComponent getComponent() { return root; }

    @Override public void loadFromSettings() {
        Integer kd = SettingsService.getInstance().get("input.keyDownDelayMs", Integer.class);
        Integer ku = SettingsService.getInstance().get("input.keyUpDelayMs", Integer.class);
        Integer actionDefaultTimeout = SettingsService.getInstance().get("action.defaultTimeoutMillis", Integer.class);
        Integer groupWaitMs = SettingsService.getInstance().get("assertion.groupWaitMs", Integer.class);
        Integer eachWaitMs  = SettingsService.getInstance().get("assertion.eachWaitMs", Integer.class);
        Integer beforeEachAfter = SettingsService.getInstance().get("beforeEach.afterWaitMs", Integer.class);

        spKeyDown.setValue(kd != null ? kd : 10);
        spKeyUp.setValue(ku != null ? ku : 30);
        spActionDefaultTimeoutMs.setValue(actionDefaultTimeout != null ? actionDefaultTimeout : 30000);
        spAssertGroupWait.setValue(groupWaitMs != null ? (groupWaitMs/1000.0) : 3.0);
        spAssertEachWait.setValue(eachWaitMs != null ? (eachWaitMs/1000.0) : 0.0);
        spBeforeEachAfterWait.setValue(beforeEachAfter != null ? (beforeEachAfter/1000.0) : 0.0);
    }

    @Override public void putTo(Map<String, Object> out) throws IllegalArgumentException {
        int kdVal = ((Number) spKeyDown.getValue()).intValue(); if (kdVal < 0) throw new IllegalArgumentException("Delays dürfen nicht negativ sein.");
        int kuVal = ((Number) spKeyUp.getValue()).intValue();   if (kuVal < 0) throw new IllegalArgumentException("Delays dürfen nicht negativ sein.");
        int actionTimeout = ((Number) spActionDefaultTimeoutMs.getValue()).intValue(); if (actionTimeout < 0) throw new IllegalArgumentException("Default Action-Timeout darf nicht negativ sein.");
        double groupS = ((Number) spAssertGroupWait.getValue()).doubleValue(); if (groupS < 0) throw new IllegalArgumentException("Wartezeit darf nicht negativ sein.");
        double eachS  = ((Number) spAssertEachWait.getValue()).doubleValue();  if (eachS < 0) throw new IllegalArgumentException("Wartezeit darf nicht negativ sein.");
        double beS    = ((Number) spBeforeEachAfterWait.getValue()).doubleValue(); if (beS < 0) throw new IllegalArgumentException("Wartezeit darf nicht negativ sein.");

        out.put("input.keyDownDelayMs", kdVal);
        out.put("input.keyUpDelayMs", kuVal);
        out.put("action.defaultTimeoutMillis", actionTimeout);
        out.put("assertion.groupWaitMs", (int)Math.round(groupS*1000));
        out.put("assertion.eachWaitMs",  (int)Math.round(eachS*1000));
        out.put("beforeEach.afterWaitMs", (int)Math.round(beS*1000));

        // Live übernehmen für Delays
        InputDelaysConfig.setKeyDownDelayMs(kdVal);
        InputDelaysConfig.setKeyUpDelayMs(kuVal);
    }

    private static GridBagConstraints gbc(){ GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(6,8,6,8); return gbc; }
    private static TitledBorder section(String t){ TitledBorder tb=BorderFactory.createTitledBorder(t); tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD)); return tb; }
}

