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
    // Neu: Aktionsdauer und Zusatzwartezeit
    private JSpinner spActionMinDurationS, spActionMaxDurationS, spActionExtraWaitS;
    private JCheckBox cbEarlyFinishAllowed;

    public InputSettingsPanel() {
        root = new JPanel(new GridBagLayout());
        root.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        GridBagConstraints g = gbc();

        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(section("Wiedergabe"));
        GridBagConstraints gb = gbc();
        gb.fill = GridBagConstraints.HORIZONTAL;

        spKeyDown = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 120.0, 0.01));
        spKeyUp   = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 120.0, 0.01));
        spActionDefaultTimeoutMs = new JSpinner(new SpinnerNumberModel(30.0, 0.0, 3600.0, 1));
        spAssertGroupWait = new JSpinner(new SpinnerNumberModel(3.0, 0.0, 3600.0, 0.1));
        spAssertEachWait  = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 3600.0, 0.1));
        spBeforeEachAfterWait = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 3600.0, 0.1));

        // --- Neue Felder: Min/Max Aktionsdauer und vorzeitiges Beenden ---
        spActionMinDurationS = new JSpinner(new SpinnerNumberModel(0.1, 0.0, 3600.0, 0.1));
        spActionMinDurationS.setEditor(new JSpinner.NumberEditor(spActionMinDurationS, "0.###"));
        spActionMinDurationS.setToolTipText("Zeit, die nach einer Aktion mindestens vergehen muss, bevor die nächste startet (Sekunden, Schritt 0.1s)");

        spActionMaxDurationS = new JSpinner(new SpinnerNumberModel(15.0, 0.0, 86400.0, 1.0));
        spActionMaxDurationS.setEditor(new JSpinner.NumberEditor(spActionMaxDurationS, "0.###"));
        spActionMaxDurationS.setToolTipText("Maximal erlaubte Dauer einer Aktion (Sekunden, Schritt 1s)");

        cbEarlyFinishAllowed = new JCheckBox("Vorzeitiges Beenden zulassen");
        cbEarlyFinishAllowed.setToolTipText("Endzeit wird automatisch ermittelt (nicht immer korrekt). Eine zusätzliche Zeit wird empfohlen.");

        spActionExtraWaitS = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 3600.0, 0.01));
        spActionExtraWaitS.setEditor(new JSpinner.NumberEditor(spActionExtraWaitS, "0.###"));
        spActionExtraWaitS.setToolTipText("Zusätzliche Wartezeit nach automatisch erkannter Endzeit (Sekunden, Schritt 0.01s)");
        spActionExtraWaitS.setEnabled(false);
        cbEarlyFinishAllowed.addItemListener(e -> {
            boolean on = cbEarlyFinishAllowed.isSelected();
            spActionExtraWaitS.setEnabled(on);
        });

        // Tooltips anpassen
        spKeyDown.setToolTipText("Verzögerung nach keyDown in Sekunden (Schritt 10ms)");
        spKeyUp.setToolTipText("Verzögerung nach keyUp in Sekunden (Schritt 10ms)");
        spActionDefaultTimeoutMs.setToolTipText("Default Action-Timeout in Sekunden (0 = kein Warten, Schritt 1s)");
        spAssertGroupWait.setToolTipText("Globale Wartezeit vor Assertions (Sekunden, Schritt 100ms)");
        spAssertEachWait.setToolTipText("Wartezeit zwischen Assertions (Sekunden, Schritt 100ms)");
        spBeforeEachAfterWait.setToolTipText("Wartezeit nach BeforeEach (Sekunden, Schritt 100ms)");

        gb.gridx=0; gb.gridy=0; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("KeyDown-Delay (s):"), gb);
        gb.gridx=1; gb.gridy=0; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spKeyDown, gb);
        gb.gridx=0; gb.gridy=1; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("KeyUp-Delay (s):"), gb);
        gb.gridx=1; gb.gridy=1; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spKeyUp, gb);

        gb.gridx=0; gb.gridy=2; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Default Action-Timeout (s):"), gb);
        gb.gridx=1; gb.gridy=2; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spActionDefaultTimeoutMs, gb);

        gb.gridx=0; gb.gridy=3; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Wartezeit vor Assertions (s):"), gb);
        gb.gridx=1; gb.gridy=3; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spAssertGroupWait, gb);
        gb.gridx=0; gb.gridy=4; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Wartezeit zwischen Assertions (s):"), gb);
        gb.gridx=1; gb.gridy=4; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spAssertEachWait, gb);
        gb.gridx=0; gb.gridy=5; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Wartezeit nach BeforeEach (s):"), gb);
        gb.gridx=1; gb.gridy=5; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spBeforeEachAfterWait, gb);

        gb.gridx=0; gb.gridy=6; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Min. Aktionsdauer (s):"), gb);
        gb.gridx=1; gb.gridy=6; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spActionMinDurationS, gb);

        gb.gridx=0; gb.gridy=7; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Max. Aktionsdauer (s):"), gb);
        gb.gridx=1; gb.gridy=7; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spActionMaxDurationS, gb);

        gb.gridx=0; gb.gridy=8; gb.gridwidth=2; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(cbEarlyFinishAllowed, gb); gb.gridwidth=1;

        gb.gridx=0; gb.gridy=9; gb.weightx=0; gb.anchor=GridBagConstraints.WEST; pnl.add(new JLabel("Zusätzliche Wartezeit (s):"), gb);
        gb.gridx=1; gb.gridy=9; gb.weightx=1; gb.anchor=GridBagConstraints.WEST; pnl.add(spActionExtraWaitS, gb);

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
        Integer minDurMs = SettingsService.getInstance().get("action.minDurationMillis", Integer.class);
        Integer maxDurMs = SettingsService.getInstance().get("action.maxDurationMillis", Integer.class);
        Boolean earlyFinish = SettingsService.getInstance().get("action.earlyFinishAllowed", Boolean.class);
        Integer extraWaitMs = SettingsService.getInstance().get("action.extraWaitMillis", Integer.class);

        // Umrechnung ms->s (Double mit 3 Nachkommastellen)
        spKeyDown.setValue(kd != null ? kd / 1000.0 : 0.01); // minimal bessere Erkennung
        spKeyUp.setValue(ku != null ? ku / 1000.0 : 0.03);
        spActionDefaultTimeoutMs.setValue(actionDefaultTimeout != null ? actionDefaultTimeout / 1000.0 : 30.0);
        spAssertGroupWait.setValue(groupWaitMs != null ? groupWaitMs / 1000.0 : 3.0);
        spAssertEachWait.setValue(eachWaitMs != null ? eachWaitMs / 1000.0 : 0.0);
        spBeforeEachAfterWait.setValue(beforeEachAfter != null ? beforeEachAfter / 1000.0 : 0.0);

        // Neue Felder
        spActionMinDurationS.setValue(minDurMs != null ? minDurMs / 1000.0 : 0.1);
        spActionMaxDurationS.setValue(maxDurMs != null ? maxDurMs / 1000.0 : 15.0);
        boolean ef = earlyFinish != null ? earlyFinish : false;
        cbEarlyFinishAllowed.setSelected(ef);
        spActionExtraWaitS.setValue(extraWaitMs != null ? extraWaitMs / 1000.0 : 0.0);
        spActionExtraWaitS.setEnabled(ef);
    }

    @Override public void putTo(Map<String, Object> out) throws IllegalArgumentException {
        double kdSec = ((Number) spKeyDown.getValue()).doubleValue(); if (kdSec < 0) throw new IllegalArgumentException("Delays dürfen nicht negativ sein.");
        double kuSec = ((Number) spKeyUp.getValue()).doubleValue();   if (kuSec < 0) throw new IllegalArgumentException("Delays dürfen nicht negativ sein.");
        double actionTimeoutSec = ((Number) spActionDefaultTimeoutMs.getValue()).doubleValue(); if (actionTimeoutSec < 0) throw new IllegalArgumentException("Default Action-Timeout darf nicht negativ sein.");
        double groupS = ((Number) spAssertGroupWait.getValue()).doubleValue(); if (groupS < 0) throw new IllegalArgumentException("Wartezeit darf nicht negativ sein.");
        double eachS  = ((Number) spAssertEachWait.getValue()).doubleValue();  if (eachS < 0) throw new IllegalArgumentException("Wartezeit darf nicht negativ sein.");
        double beS    = ((Number) spBeforeEachAfterWait.getValue()).doubleValue(); if (beS < 0) throw new IllegalArgumentException("Wartezeit darf nicht negativ sein.");
        // Neue Felder
        double minDurS = ((Number) spActionMinDurationS.getValue()).doubleValue(); if (minDurS < 0) throw new IllegalArgumentException("Min. Aktionsdauer darf nicht negativ sein.");
        double maxDurS = ((Number) spActionMaxDurationS.getValue()).doubleValue(); if (maxDurS < 0) throw new IllegalArgumentException("Max. Aktionsdauer darf nicht negativ sein.");
        if (maxDurS > 0 && minDurS > maxDurS) throw new IllegalArgumentException("Min. Aktionsdauer darf nicht größer als Max. Aktionsdauer sein.");
        double extraWaitS = ((Number) spActionExtraWaitS.getValue()).doubleValue(); if (extraWaitS < 0) throw new IllegalArgumentException("Zusätzliche Wartezeit darf nicht negativ sein.");

        // Umrechnung s->ms
        int kdMs  = (int)Math.round(kdSec * 1000.0);
        int kuMs  = (int)Math.round(kuSec * 1000.0);
        int actionMs = (int)Math.round(actionTimeoutSec * 1000.0);
        int groupMs  = (int)Math.round(groupS * 1000.0);
        int eachMs   = (int)Math.round(eachS * 1000.0);
        int beMs     = (int)Math.round(beS * 1000.0);
        int minMs    = (int)Math.round(minDurS * 1000.0);
        int maxMs    = (int)Math.round(maxDurS * 1000.0);
        int extraMs  = (int)Math.round(extraWaitS * 1000.0);

        out.put("input.keyDownDelayMs", kdMs);
        out.put("input.keyUpDelayMs", kuMs);
        out.put("action.defaultTimeoutMillis", actionMs);
        out.put("assertion.groupWaitMs", groupMs);
        out.put("assertion.eachWaitMs",  eachMs);
        out.put("beforeEach.afterWaitMs", beMs);
        out.put("action.minDurationMillis", minMs);
        out.put("action.maxDurationMillis", maxMs);
        out.put("action.earlyFinishAllowed", cbEarlyFinishAllowed.isSelected());
        out.put("action.extraWaitMillis", extraMs);

        // Live übernehmen für Delays
        InputDelaysConfig.setKeyDownDelayMs(kdMs);
        InputDelaysConfig.setKeyUpDelayMs(kuMs);
    }

    private static GridBagConstraints gbc(){ GridBagConstraints gbc=new GridBagConstraints(); gbc.insets=new Insets(6,8,6,8); return gbc; }
    private static TitledBorder section(String t){ TitledBorder tb=BorderFactory.createTitledBorder(t); tb.setTitleFont(tb.getTitleFont().deriveFont(Font.BOLD)); return tb; }
}
