package de.bund.zrb.ui.commands;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.StatusMessageEvent;
import de.bund.zrb.service.SettingsService;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.video.overlay.VideoOverlayService;
import de.bund.zrb.video.overlay.VideoOverlayStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Öffnet ein Preview-Fenster in Videoauflösung, in dem Overlay-Platzhalter frei positioniert und gestaltet werden können. */
public class OpenVideoOverlaySettingsCommand extends ShortcutMenuCommand {

    @Override public String getId() { return "record.video.overlay"; }
    @Override public String getLabel() { return "Overlay-Einstellungen"; }

    @Override
    public void perform() {
        SwingUtilities.invokeLater(() -> {
            try {
                VideoOverlayService svc = VideoOverlayService.getInstance();

                // Zielauflösung aus Settings lesen (wie Recording-Settings), Fallback auf 1280x720
                int w = getInt("recording.video.width", 1280);
                int h = getInt("recording.video.height", 720);
                if (w <= 0) w = 1280;
                if (h <= 0) h = 720;
                Dimension videoSize = new Dimension(w, h);

                JFrame frame = new JFrame("Overlay-Layout (Preview)");
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.setUndecorated(true); // rahmenlos
                frame.setLayout(new BorderLayout());

                OverlayPreviewPanel preview = new OverlayPreviewPanel(svc, videoSize);
                frame.add(preview, BorderLayout.CENTER);

                // Einfache Steuerleiste oben rechts für Schließen
                JToolBar bar = new JToolBar();
                bar.setFloatable(false);
                bar.setOpaque(false);
                JButton close = new JButton("X");
                close.addActionListener(e -> frame.dispose());
                bar.add(Box.createHorizontalGlue());
                bar.add(close);
                frame.add(bar, BorderLayout.NORTH);

                frame.setSize(videoSize);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } catch (Exception ex) {
                ApplicationEventBus.getInstance().publish(new StatusMessageEvent(
                        "Overlay-Dialog konnte nicht geöffnet werden: " + ex.getMessage(), 4000));
            }
        });
    }

    private static int getInt(String key, int def) {
        Integer v = SettingsService.getInstance().get(key, Integer.class);
        return v == null ? def : v.intValue();
    }

    /**
     * Panel, das eine Videoauflösung simuliert und mehrere verschiebbare Text-Platzhalter enthält.
     * Jeder Platzhalter kann einem bestehenden Overlay-Typ (Caption/Subtitle/Action) zugeordnet werden
     * und erlaubt die Konfiguration von Schriftfarbe, Hintergrundfarbe (inkl. Transparenz) und Rahmen.
     */
    static final class OverlayPreviewPanel extends JLayeredPane {
        private final VideoOverlayService service;
        private final Dimension videoSize;
        private final List<Placeholder> placeholders = new ArrayList<>();
        private final Timer visualizerTimer;
        private float phase;
        private final Random rnd = new Random();

        OverlayPreviewPanel(VideoOverlayService service, Dimension videoSize) {
            this.service = service;
            this.videoSize = videoSize;
            setPreferredSize(videoSize);
            setLayout(null); // absolute Positionierung

            setBackground(Color.DARK_GRAY);
            setOpaque(true);

            // Platzhalter anhand gespeicherter prozentualer Positionen anlegen
            addPlaceholder("Caption", Placeholder.Kind.CAPTION, service.getCaptionStyle(),
                    "video.overlay.caption.posX", "video.overlay.caption.posY", 0.05, 0.05);
            addPlaceholder("Subtitle", Placeholder.Kind.SUBTITLE, service.getSubtitleStyle(),
                    "video.overlay.subtitle.posX", "video.overlay.subtitle.posY", 0.05, 0.75);
            addPlaceholder("Action", Placeholder.Kind.ACTION, service.getActionStyle(),
                    "video.overlay.action.posX", "video.overlay.action.posY", 0.75, 0.05);

            // Einfacher Visualizer: animierte Sinuslinien im Hintergrund, mit sehr wenig Code
            visualizerTimer = new Timer(40, e -> {
                phase += 0.08f;
                repaint();
            });
            visualizerTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            int w = getWidth();
            int h = getHeight();

            // Weicher Hintergrundverlauf als Basis
            g2.setPaint(new GradientPaint(0, 0,
                    new Color(20, 20, 30),
                    w, h,
                    new Color(10, 10, 60)));
            g2.fillRect(0, 0, w, h);

            // Künstlerischer Visualizer:
            // 1) Halbtransparente farbige Bänder (Polylines) über die Breite
            g2.setStroke(new BasicStroke(2f));
            int bands = 6;
            for (int i = 0; i < bands; i++) {
                float hue = (phase / 10f + i * 0.12f) % 1f;
                Color base = Color.getHSBColor(hue, 0.7f, 0.95f);
                Color bandColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), 120);
                g2.setColor(bandColor);

                int prevX = 0;
                int prevY = (int) (h * (0.2 + 0.6 * (i / (double) bands))) + (int) (Math.sin(phase + i) * h * 0.05);
                for (int x = 1; x < w; x += 4) {
                    double t = (double) x / (double) w;
                    double noise = Math.sin(t * Math.PI * 6 + phase * 1.3 + i * 0.7)
                                   + 0.5 * Math.sin(t * Math.PI * 11 - phase * 0.9 + i);
                    int y = (int) (prevY + noise * (h * 0.03));
                    g2.drawLine(prevX, prevY, x, y);
                    prevX = x;
                    prevY = y;
                }
            }

            // 2) Sanft pulsierende, bunte Kreise im Hintergrund (wie eine abstrakte Lichtshow)
            int circles = 18;
            for (int i = 0; i < circles; i++) {
                double t = (phase * 0.25 + i * 0.37);
                float hue = (float) ((t * 0.21) % 1.0);
                Color c = Color.getHSBColor(hue, 0.8f, 1.0f);
                int radius = (int) (Math.abs(Math.sin(t * 1.7)) * Math.min(w, h) * 0.2) + 40;

                // Position leicht zufällig, aber durch phase verschoben
                int cx = (int) ((0.2 + 0.6 * Math.abs(Math.sin(t * 0.9))) * w);
                int cy = (int) ((0.2 + 0.6 * Math.abs(Math.cos(t * 1.1))) * h);

                // Sehr transparente Füllung, damit sich die Kreise überlagern
                Color fill = new Color(c.getRed(), c.getGreen(), c.getBlue(), 40);
                g2.setPaint(new RadialGradientPaint(
                        new Point(cx, cy), radius,
                        new float[]{0f, 1f},
                        new Color[]{fill, new Color(0,0,0,0)}));
                g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            }

            // 3) Dezentes Scanline-Overlay für einen leicht „digitalen“ Look
            g2.setColor(new Color(255, 255, 255, 10));
            for (int y = 0; y < h; y += 4) {
                g2.drawLine(0, y, w, y);
            }

            g2.dispose();
        }

        private void addPlaceholder(String label, Placeholder.Kind kind, VideoOverlayStyle style,
                                    String keyX, String keyY,
                                    double defaultPx, double defaultPy) {
            double px = getDouble(keyX, defaultPx);
            double py = getDouble(keyY, defaultPy);
            if (px < 0 || px > 1) px = defaultPx;
            if (py < 0 || py > 1) py = defaultPy;

            Placeholder p = new Placeholder(label, kind, style, service, this, keyX, keyY);
            int w = 280;
            int h = 80;
            int x = (int) Math.round(px * (videoSize.width - w));
            int y = (int) Math.round(py * (videoSize.height - h));
            p.setBounds(x, y, w, h);
            placeholders.add(p);
            add(p, JLayeredPane.PALETTE_LAYER);
        }

        private static double getDouble(String key, double def) {
            Double d = SettingsService.getInstance().get(key, Double.class);
            if (d == null) {
                // evtl. als String gespeichert
                String s = SettingsService.getInstance().get(key, String.class);
                if (s != null) {
                    try { d = Double.valueOf(s.trim()); } catch (NumberFormatException ignored) { }
                }
            }
            return d == null ? def : d.doubleValue();
        }
    }

    /**
     * Einzelner verschieb- und konfigurierbarer Overlay-Platzhalter.
     */
    static final class Placeholder extends JPanel {
        enum Kind { CAPTION, SUBTITLE, ACTION }

        private final Kind kind;
        private final VideoOverlayService service;
        private final String keyPosX;
        private final String keyPosY;
        private Point dragOffset;

        Placeholder(String label, Kind kind, VideoOverlayStyle style,
                    VideoOverlayService service, JComponent parent,
                    String keyPosX, String keyPosY) {
            this.kind = kind;
            this.service = service;
            this.keyPosX = keyPosX;
            this.keyPosY = keyPosY;
            setLayout(new BorderLayout(4,4));

            setOpaque(false);
            applyStyle(style);

            JLabel title = new JLabel(label);
            title.setFont(title.getFont().deriveFont(Font.BOLD));
            add(title, BorderLayout.NORTH);

            JTextArea area = new JTextArea("Beispieltext " + label);
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setOpaque(false);
            area.setEditable(false);
            add(area, BorderLayout.CENTER);

            JButton edit = new JButton("Design / Typ...");
            edit.addActionListener(e -> openConfigDialog(parent));
            add(edit, BorderLayout.SOUTH);

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    dragOffset = e.getPoint();
                }
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragOffset == null) return;
                    int nx = getX() + e.getX() - dragOffset.x;
                    int ny = getY() + e.getY() - dragOffset.y;
                    setLocation(nx, ny);
                    // Position als Prozent der Videoauflösung speichern
                    Dimension size = parent.getSize();
                    double px = (double) nx / (double) Math.max(1, size.width - getWidth());
                    double py = (double) ny / (double) Math.max(1, size.height - getHeight());
                    SettingsService.getInstance().set(keyPosX, px);
                    SettingsService.getInstance().set(keyPosY, py);
                    parent.repaint();
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private void applyStyle(VideoOverlayStyle style) {
            if (style == null) return;
            try {
                Color font = Color.decode(style.getFontColor());
                Color bg = Color.decode(style.getBackgroundColor());
                // Hintergrund leicht transparent zeichnen
                Color bgTrans = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 180);
                setBackground(bgTrans);
                setOpaque(true);
                setForeground(font);
                setBorder(BorderFactory.createLineBorder(font));
            } catch (Exception ignored) {
                setOpaque(false);
                setBorder(BorderFactory.createDashedBorder(Color.LIGHT_GRAY));
            }
            revalidate();
            repaint();
        }

        private void openConfigDialog(Component parent) {
            VideoOverlayStyle current = getCurrentStyle();
            JTextField tfFontColor = new JTextField(current.getFontColor());
            JTextField tfBgColor = new JTextField(current.getBackgroundColor());
            JSpinner spFontSize = new JSpinner(new SpinnerNumberModel(current.getFontSizePx(), 8, 96, 1));

            String[] types = {"Caption", "Subtitle", "Action"};
            JComboBox<String> cbType = new JComboBox<>(types);
            cbType.setSelectedIndex(kind == Kind.CAPTION ? 0 : kind == Kind.SUBTITLE ? 1 : 2);

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4,4,4,4);
            c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST;
            panel.add(new JLabel("Overlay-Typ:"), c);
            c.gridx = 1; panel.add(cbType, c);

            c.gridx = 0; c.gridy = 1; panel.add(new JLabel("Font Color (#RRGGBB):"), c);
            c.gridx = 1; panel.add(tfFontColor, c);

            c.gridx = 0; c.gridy = 2; panel.add(new JLabel("Background Color (#RRGGBB):"), c);
            c.gridx = 1; panel.add(tfBgColor, c);

            c.gridx = 0; c.gridy = 3; panel.add(new JLabel("Font Size (px):"), c);
            c.gridx = 1; panel.add(spFontSize, c);

            int res = JOptionPane.showConfirmDialog(parent, panel,
                    "Overlay-Platzhalter konfigurieren", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res == JOptionPane.OK_OPTION) {
                VideoOverlayStyle newStyle = new VideoOverlayStyle(
                        tfFontColor.getText().trim(),
                        tfBgColor.getText().trim(),
                        (Integer) spFontSize.getValue());

                String sel = (String) cbType.getSelectedItem();
                Kind newKind = kind;
                if ("Caption".equals(sel)) newKind = Kind.CAPTION;
                else if ("Subtitle".equals(sel)) newKind = Kind.SUBTITLE;
                else if ("Action".equals(sel)) newKind = Kind.ACTION;

                applyStyle(newStyle);
                applyToService(newKind, newStyle);
            }
        }

        private VideoOverlayStyle getCurrentStyle() {
            switch (kind) {
                case CAPTION:
                    return service.getCaptionStyle();
                case SUBTITLE:
                    return service.getSubtitleStyle();
                case ACTION:
                default:
                    return service.getActionStyle();
            }
        }

        private void applyToService(Kind target, VideoOverlayStyle style) {
            switch (target) {
                case CAPTION:
                    service.applyCaptionStyle(style);
                    service.setCaptionEnabled(true);
                    break;
                case SUBTITLE:
                    service.applySubtitleStyle(style);
                    service.setSubtitleEnabled(true);
                    break;
                case ACTION:
                    service.applyActionStyle(style);
                    service.setActionTransientEnabled(true);
                    break;
            }
        }
    }
}
