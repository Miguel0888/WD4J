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

            // Ruhigere Animation: langsamere Phase und weichere Bewegung
            visualizerTimer = new Timer(70, e -> {
                phase += 0.04f;
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
                    new Color(25, 25, 35),
                    w, h,
                    new Color(5, 5, 25)));
            g2.fillRect(0, 0, w, h);

            // 1) Breite, sanft schwingende Farbbänder
            g2.setStroke(new BasicStroke(1.5f));
            int bands = 4;
            for (int i = 0; i < bands; i++) {
                float hue = (phase / 12f + i * 0.18f) % 1f;
                Color base = Color.getHSBColor(hue, 0.5f, 0.8f);
                Color bandColor = new Color(base.getRed(), base.getGreen(), base.getBlue(), 80);
                g2.setColor(bandColor);

                int prevX = 0;
                int prevY = (int) (h * (0.3 + 0.4 * (i / (double) bands)));
                for (int x = 1; x < w; x += 5) {
                    double t = (double) x / (double) w;
                    double yOff = Math.sin(t * Math.PI * 2 + phase * 0.8 + i * 0.6) * (h * 0.04);
                    int y = prevY + (int) yOff;
                    g2.drawLine(prevX, prevY, x, y);
                    prevX = x;
                    prevY = y;
                }
            }

            // 2) Ruhige, weiche Lichtflecken (Kreise)
            int circles = 10;
            for (int i = 0; i < circles; i++) {
                double t = (phase * 0.15 + i * 0.45);
                float hue = (float) ((t * 0.17) % 1.0);
                Color c = Color.getHSBColor(hue, 0.4f, 0.7f);
                int radius = (int) (Math.abs(Math.sin(t * 1.3)) * Math.min(w, h) * 0.15) + 30;
                int cx = (int) ((0.1 + 0.8 * Math.abs(Math.sin(t * 0.7))) * w);
                int cy = (int) ((0.1 + 0.8 * Math.abs(Math.cos(t * 0.9))) * h);

                Color fill = new Color(c.getRed(), c.getGreen(), c.getBlue(), 25);
                g2.setPaint(new RadialGradientPaint(
                        new Point(cx, cy), radius,
                        new float[]{0f, 1f},
                        new Color[]{fill, new Color(0,0,0,0)}));
                g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            }

            // dezente Scanlines sehr schwach
            g2.setColor(new Color(255, 255, 255, 8));
            for (int y = 0; y < h; y += 5) {
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
            edit.setToolTipText("Schriftart, Farben, Hintergrund und Typ des Overlays anpassen");
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
                Color bg;
                int alpha = 180;
                String bgStr = style.getBackgroundColor();
                if (bgStr != null && bgStr.toLowerCase().startsWith("rgba")) {
                    // einfaches rgba( r, g, b, a )-Parsing
                    int start = bgStr.indexOf('(');
                    int end = bgStr.indexOf(')');
                    if (start >= 0 && end > start) {
                        String[] parts = bgStr.substring(start + 1, end).split(",");
                        if (parts.length >= 4) {
                            int r = Integer.parseInt(parts[0].trim());
                            int g = Integer.parseInt(parts[1].trim());
                            int b = Integer.parseInt(parts[2].trim());
                            float af = Float.parseFloat(parts[3].trim());
                            alpha = (int) Math.round(255 * af);
                            bg = new Color(r, g, b);
                        } else {
                            bg = Color.decode("#000000");
                        }
                    } else {
                        bg = Color.decode("#000000");
                    }
                } else {
                    bg = Color.decode(style.getBackgroundColor());
                }
                Color bgTrans = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), alpha);
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

            // Aktuelle Farben und Transparenz vorbereiten
            Color initialFont;
            Color initialBg;
            int initialAlpha = 180;
            try {
                initialFont = Color.decode(current.getFontColor());
            } catch (Exception e) {
                initialFont = getForeground();
            }
            try {
                String bgStr = current.getBackgroundColor();
                if (bgStr != null && bgStr.toLowerCase().startsWith("rgba")) {
                    int start = bgStr.indexOf('(');
                    int end = bgStr.indexOf(')');
                    if (start >= 0 && end > start) {
                        String[] parts = bgStr.substring(start + 1, end).split(",");
                        if (parts.length >= 4) {
                            int r = Integer.parseInt(parts[0].trim());
                            int g = Integer.parseInt(parts[1].trim());
                            int b = Integer.parseInt(parts[2].trim());
                            float af = Float.parseFloat(parts[3].trim());
                            initialAlpha = (int) Math.round(255 * af);
                            initialBg = new Color(r, g, b);
                        } else {
                            initialBg = getBackground();
                        }
                    } else {
                        initialBg = getBackground();
                    }
                } else {
                    initialBg = Color.decode(current.getBackgroundColor());
                }
            } catch (Exception e) {
                initialBg = getBackground();
            }

            // Farbfelder mit Vorschau
            JButton fontColorPreview = new JButton();
            fontColorPreview.setPreferredSize(new Dimension(24, 24));
            fontColorPreview.setBackground(initialFont);
            fontColorPreview.setToolTipText("Textfarbe auswählen");

            JButton bgColorPreview = new JButton();
            bgColorPreview.setPreferredSize(new Dimension(24, 24));
            bgColorPreview.setBackground(initialBg);
            bgColorPreview.setToolTipText("Hintergrundfarbe auswählen");

            final Color[] chosenFont = { initialFont };
            final Color[] chosenBg = { initialBg };

            fontColorPreview.addActionListener(e -> {
                Color c = JColorChooser.showDialog(parent, "Textfarbe wählen", chosenFont[0]);
                if (c != null) {
                    chosenFont[0] = c;
                    fontColorPreview.setBackground(c);
                }
            });
            bgColorPreview.addActionListener(e -> {
                Color c = JColorChooser.showDialog(parent, "Hintergrundfarbe wählen", chosenBg[0]);
                if (c != null) {
                    chosenBg[0] = c;
                    bgColorPreview.setBackground(c);
                }
            });

            // Transparenz-Slider mit sofortiger Vorschau
            JSlider alphaSlider = new JSlider(0, 255, initialAlpha);
            alphaSlider.setPaintTicks(true);
            alphaSlider.setPaintLabels(true);
            alphaSlider.setMajorTickSpacing(85); // 0 / ca. 1/3 / 2/3 / 1

            alphaSlider.addChangeListener(e -> {
                Color b = chosenBg[0];
                if (b != null) {
                    int a = alphaSlider.getValue();
                    Color bgTrans = new Color(b.getRed(), b.getGreen(), b.getBlue(), a);
                    setBackground(bgTrans);
                    setOpaque(true);
                    repaint();
                }
            });

            // Typ-Auswahl und Schriftgröße wie bisher
            String[] types = {"Caption", "Subtitle", "Action"};
            JComboBox<String> cbType = new JComboBox<>(types);
            cbType.setSelectedIndex(kind == Kind.CAPTION ? 0 : kind == Kind.SUBTITLE ? 1 : 2);

            JSpinner spFontSize = new JSpinner(new SpinnerNumberModel(current.getFontSizePx(), 8, 96, 1));

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(4,4,4,4);
            c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST;
            panel.add(new JLabel("Overlay-Typ:"), c);
            c.gridx = 1; panel.add(cbType, c);

            c.gridx = 0; c.gridy = 1; panel.add(new JLabel("Textfarbe:"), c);
            c.gridx = 1; panel.add(fontColorPreview, c);

            c.gridx = 0; c.gridy = 2; panel.add(new JLabel("Hintergrundfarbe:"), c);
            c.gridx = 1; panel.add(bgColorPreview, c);

            c.gridx = 0; c.gridy = 3; panel.add(new JLabel("Hintergrund-Transparenz:"), c);
            c.gridx = 1; panel.add(alphaSlider, c);

            c.gridx = 0; c.gridy = 4; panel.add(new JLabel("Schriftgröße (px):"), c);
            c.gridx = 1; panel.add(spFontSize, c);

            int res = JOptionPane.showConfirmDialog(parent, panel,
                    "Overlay-Platzhalter konfigurieren", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res == JOptionPane.OK_OPTION) {
                Color f = chosenFont[0];
                Color b = chosenBg[0];
                int a = alphaSlider.getValue();
                if (f == null || b == null) {
                    return;
                }
                String fontHex = String.format("#%02X%02X%02X", f.getRed(), f.getGreen(), f.getBlue());
                String bgRgba = String.format("rgba(%d,%d,%d,%.3f)", b.getRed(), b.getGreen(), b.getBlue(), a / 255.0);

                VideoOverlayStyle newStyle = new VideoOverlayStyle(
                        fontHex,
                        bgRgba,
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
