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

        // --- Speed slider ---
        double min = getDouble("playback.speed.min", 0.1);
        double max = getDouble("playback.speed.max", 2.0);
        double cur = getDouble("playback.speed.current", min); // Default jetzt min statt 1.0
        if (max <= min) { max = Math.max(min + 0.1, 1.0); }
        cur = clamp(cur, min, max);

        speedSlider = createSpeedSlider(min, max, cur);
        speedSlider.setToolTipText("Ausführungsgeschwindigkeit / Zeitlupe (" + fmt(cur) + "x)");
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

    private static String fmt(double d){
        String s = String.format(java.util.Locale.ENGLISH, "%.2f", d);
        // trim trailing zeros
        while (s.contains(".") && (s.endsWith("0") || s.endsWith("."))) {
            s = s.endsWith("0") ? s.substring(0, s.length()-1) : s.substring(0, s.length()-1);
        }
        return s;
    }

    private JSlider createSpeedSlider(double min, double max, double cur) {
        // Map double range [min,max] to slider [0..1000]
        int SLIDER_MAX = 1000;
        int value = (int) Math.round((cur - min) / (max - min) * SLIDER_MAX);
        JSlider s = new JSlider(0, SLIDER_MAX, value);
        s.setFocusable(false);
        s.setPreferredSize(new Dimension(120, 22));
        s.addChangeListener(e -> {
            int v = ((JSlider) e.getSource()).getValue();
            double factor = min + (v / (double) SLIDER_MAX) * (max - min);
            // persist current factor
            SettingsService.getInstance().set("playback.speed.current", factor);
            s.setToolTipText("Ausführungsgeschwindigkeit / Zeitlupe (" + fmt(factor) + "x)");
        });
        return s;
    }
}
