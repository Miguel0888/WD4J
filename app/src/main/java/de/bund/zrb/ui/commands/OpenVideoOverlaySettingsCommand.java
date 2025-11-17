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

            // Hintergrundverlauf
            g2.setPaint(new GradientPaint(0, 0, new Color(40, 40, 40), 0, h, new Color(15, 15, 15)));
            g2.fillRect(0, 0, w, h);

            // Minimalistischer Visualizer: mehrere Sinuskurven mit Phasenverschiebung
            g2.setStroke(new BasicStroke(2f));
            for (int i = 0; i < 4; i++) {
                float hue = (phase / 10f + i * 0.15f) % 1f;
                g2.setColor(Color.getHSBColor(hue, 0.6f, 0.9f));
                int midY = h / 2 + (i - 2) * (h / 10);
                int prevX = 0;
                int prevY = midY;
                for (int x = 1; x < w; x++) {
                    double t = (double) x / (double) w;
                    double yOff = Math.sin(t * Math.PI * 4 + phase + i) * (h / 8.0);
                    int y = midY + (int) yOff;
                    g2.drawLine(prevX, prevY, x, y);
                    prevX = x;
                    prevY = y;
                }
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
