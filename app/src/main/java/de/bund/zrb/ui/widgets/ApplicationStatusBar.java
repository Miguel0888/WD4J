package de.bund.zrb.ui.widgets;

import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Composite Statusbar including:
 * - left message ticker area (StatusBar)
 * - right controls: [speed slider] [user select combo]
 */
public final class ApplicationStatusBar extends JPanel {

    private final StatusBar statusBar;
    private final JSlider speedSlider;
    private final UserSelectionCombo userCombo;

    public ApplicationStatusBar(UserRegistry userRegistry) {
        super(new BorderLayout());
        // Build right controls panel
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        rightControls.setOpaque(false);

        // --- Speed (Delay) slider in ms ---
        double minMs = getDouble("playback.delay.minMs", 0.0); // Mindestverzögerung
        double maxMs = getDouble("playback.delay.maxMs", 5000.0); // Default 5s
        // Legacy-Migration: Falls alter Faktor existiert und kein neuer currentMs
        Double legacyFactorSeconds = SettingsService.getInstance().get("playback.speed.current", Double.class);
        Double curMsObj = SettingsService.getInstance().get("playback.delay.currentMs", Double.class);
        double curMs = (curMsObj != null) ? curMsObj : (legacyFactorSeconds != null ? legacyFactorSeconds * 1000.0 : minMs);
        if (maxMs <= minMs) maxMs = minMs + 1000.0; // Sicherheitsabstand
        curMs = clamp(curMs, minMs, maxMs);
        // Persist Migrierte Werte (einmalig)
        SettingsService.getInstance().set("playback.delay.currentMs", curMs);
        // Erzeuge Slider
        speedSlider = createDelaySlider(minMs, maxMs, curMs);
        speedSlider.setToolTipText("Verzögerung nach jeder Aktion: " + (int)Math.round(curMs) + " ms");
        rightControls.add(speedSlider);

        // --- User combo ---
        userCombo = new UserSelectionCombo(Objects.requireNonNull(userRegistry));
        rightControls.add(userCombo);

        // Base status bar with right controls
        statusBar = new StatusBar(rightControls);
        add(statusBar, BorderLayout.CENTER);
    }

    public StatusBar getStatusBar() { return statusBar; }
    public JSlider getSpeedSlider() { return speedSlider; }
    public UserSelectionCombo getUserCombo() { return userCombo; }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private static double getDouble(String key, double def) {
        Double v = SettingsService.getInstance().get(key, Double.class);
        return v != null ? v.doubleValue() : def;
    }

    private static String fmt(double d){ // kann bleiben für evtl. spätere Darstellung
        String s = String.format(java.util.Locale.ENGLISH, "%.2f", d);
        // trim trailing zeros
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
            s = s.substring(0, s.length()-1);
        }
        return s;
    }

    private JSlider createDelaySlider(double minMs, double maxMs, double curMs) {
        int SLIDER_STEPS = 1000; // Auflösung
        int value = (int)Math.round((curMs - minMs) / (maxMs - minMs) * SLIDER_STEPS);
        JSlider s = new JSlider(0, SLIDER_STEPS, value);
        s.setFocusable(false);
        s.setPreferredSize(new Dimension(140, 22));
        s.addChangeListener(e -> {
            int v = ((JSlider)e.getSource()).getValue();
            double ms = minMs + (v / (double)SLIDER_STEPS) * (maxMs - minMs);
            long msRounded = Math.round(ms);
            SettingsService.getInstance().set("playback.delay.currentMs", (double) msRounded);
            s.setToolTipText("Verzögerung nach jeder Aktion: " + msRounded + " ms");
        });
        return s;
    }
}
