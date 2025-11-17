package de.bund.zrb.ui.widgets;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.SavedEntityEvent;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.service.UserRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
    private final JButton historyToggle = new JButton("^");
    private final List<String> history = new ArrayList<>();
    private JComponent overlay;

    public ApplicationStatusBar(UserRegistry userRegistry) {
        super(new BorderLayout());
        // Build right controls panel
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        rightControls.setOpaque(false);

        // Toggle '^' links neben Slider (rechtsbündig)
        historyToggle.setFocusable(false);
        historyToggle.setMargin(new Insets(0,8,0,8));
        historyToggle.setToolTipText("Verlauf anzeigen");
        historyToggle.addActionListener(e -> toggleOverlay());
        rightControls.add(historyToggle);

        // --- Speed (Delay) slider in ms ---
        Integer configuredMinInt = SettingsService.getInstance().get("action.minDurationMillis", Integer.class);
        Integer configuredMaxInt = SettingsService.getInstance().get("action.maxDurationMillis", Integer.class);
        double minMs = (configuredMinInt != null) ? configuredMinInt.doubleValue() : 100.0; // 0.1s Fallback
        double maxMs = (configuredMaxInt != null) ? configuredMaxInt.doubleValue() : 15000.0; // 15s Fallback
        // Legacy-Migration von Sekunden-Faktor falls vorhanden
        Double legacyFactorSeconds = SettingsService.getInstance().get("playback.speed.current", Double.class);
        Double curMsObj = SettingsService.getInstance().get("playback.delay.currentMs", Double.class);
        double curMs = (curMsObj != null) ? curMsObj : (legacyFactorSeconds != null ? legacyFactorSeconds * 1000.0 : minMs);
        if (maxMs <= minMs) maxMs = minMs + 1000.0; // Sicherheitsabstand falls fehlerhafte Konfig
        curMs = clamp(curMs, minMs, maxMs);
        SettingsService.getInstance().set("playback.delay.currentMs", curMs);
        speedSlider = createDelaySlider(minMs, maxMs, curMs);
        speedSlider.setToolTipText("Verzögerung nach jeder Aktion: " + (int)Math.round(curMs) + " ms (Min: " + (int)minMs + " / Max: " + (int)maxMs + ")");
        rightControls.add(speedSlider);

        // --- User combo ---
        userCombo = new UserSelectionCombo(Objects.requireNonNull(userRegistry));
        rightControls.add(userCombo);

        // Base status bar with right controls
        statusBar = new StatusBar(rightControls);
        add(statusBar, BorderLayout.CENTER);

        // Sammle SavedEntityEvents in Historie
        ApplicationEventBus.getInstance().subscribe(SavedEntityEvent.class, ev -> {
            SavedEntityEvent.Payload p = ev.getPayload();
            String time = p.timestamp == null ? "" : DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                    .format(p.timestamp.atZone(ZoneId.systemDefault()));
            String line = (time.isEmpty()?"":time+" - ") + p.entityType + ": " + (p.name!=null?p.name:"(unnamed)");
            synchronized (history) { history.add(line); }
            // Optional: Größe begrenzen
            if (history.size() > 500) { history.remove(0); }
        });
    }

    private void toggleOverlay() {
        if (overlay != null && overlay.isVisible()) {
            hideOverlay();
        } else {
            showOverlay();
        }
    }

    private void showOverlay() {
        // Host-Frame ermitteln
        Window w = SwingUtilities.getWindowAncestor(this);
        if (!(w instanceof JFrame)) return;
        JFrame frame = (JFrame) w;
        JComponent glass = (JComponent) frame.getGlassPane();
        glass.setVisible(true);
        glass.setLayout(null);
        // Außenklick schließt
        MouseAdapter closer = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { hideOverlay(); }
        };
        glass.addMouseListener(closer);

        // Overlay Panel bauen (oben, volle Breite, feste Höhe)
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,1,0,new Color(0,0,0,60)),
                BorderFactory.createEmptyBorder(8,12,8,12)
        ));
        panel.setBackground(new Color(250,250,250,240));

        // Toolbar mit Titel + Löschen
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Status-Verlauf");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        JButton clear = new JButton("Verlauf löschen");
        clear.addActionListener(e -> {
            synchronized (history) { history.clear(); }
            rebuildHistory(panel);
        });
        top.add(title, BorderLayout.WEST);
        top.add(clear, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        // History Liste
        JScrollPane scroll = new JScrollPane(buildHistoryList(),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        panel.add(scroll, BorderLayout.CENTER);

        // Größe und Position (oben überdecken, Layout nicht verschieben)
        int width = frame.getWidth();
        int height = Math.min(300, Math.max(160, frame.getHeight()/3));
        panel.setBounds(0, 0, width, height);

        glass.add(panel);
        glass.revalidate();
        glass.repaint();
        overlay = panel;

        // Konsumiere Klicks IM Panel, damit es nicht sofort schließt
        panel.addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { e.consume(); } });
        scroll.addMouseListener(new MouseAdapter() { @Override public void mousePressed(MouseEvent e) { e.consume(); } });
    }

    private void hideOverlay() {
        if (overlay == null) return;
        Window w = SwingUtilities.getWindowAncestor(this);
        if (!(w instanceof JFrame)) return;
        JFrame frame = (JFrame) w;
        JComponent glass = (JComponent) frame.getGlassPane();
        glass.setVisible(false);
        glass.removeAll();
        glass.revalidate();
        glass.repaint();
        overlay = null;
    }

    private void rebuildHistory(JPanel container) {
        // Ersetze Center mit neuer Liste
        for (Component c : container.getComponents()) {
            if (BorderLayout.CENTER.equals(((BorderLayout)container.getLayout()).getConstraints(c))) {
                container.remove(c);
                break;
            }
        }
        JScrollPane scroll = new JScrollPane(buildHistoryList(),
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        container.add(scroll, BorderLayout.CENTER);
        container.revalidate();
        container.repaint();
    }

    private JList<String> buildHistoryList() {
        DefaultListModel<String> model = new DefaultListModel<>();
        synchronized (history) {
            for (int i = 0; i < history.size(); i++) model.addElement(history.get(i));
        }
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFont(list.getFont().deriveFont(12f));
        return list;
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
        int SLIDER_STEPS = 1000;
        int value = (int)Math.round((curMs - minMs) / (maxMs - minMs) * SLIDER_STEPS);
        JSlider s = new JSlider(0, SLIDER_STEPS, value);
        s.setFocusable(false);
        s.setPreferredSize(new Dimension(160, 22));
        s.addChangeListener(e -> {
            int v = ((JSlider)e.getSource()).getValue();
            double ms = minMs + (v / (double)SLIDER_STEPS) * (maxMs - minMs);
            long msRounded = Math.round(ms);
            // Clamp gegen aktuelle Min/Max aus Settings (falls während Lauf geändert)
            Integer dynMin = SettingsService.getInstance().get("action.minDurationMillis", Integer.class);
            Integer dynMax = SettingsService.getInstance().get("action.maxDurationMillis", Integer.class);
            if (dynMin != null && msRounded < dynMin) msRounded = dynMin.longValue();
            if (dynMax != null && msRounded > dynMax) msRounded = dynMax.longValue();
            SettingsService.getInstance().set("playback.delay.currentMs", (double) msRounded);
            s.setToolTipText("Verzögerung nach jeder Aktion: " + msRounded + " ms (Min: " + (dynMin!=null?dynMin: (int)minMs) + " / Max: " + (dynMax!=null?dynMax: (int)maxMs) + ")");
        });
        return s;
    }
}
