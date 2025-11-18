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
import java.awt.image.BufferedImage;
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

            // Labels angepasst: Suite/Case/Action (statt Caption/Subtitle)
            addPlaceholder("Suite", Placeholder.Kind.CAPTION, service.getCaptionStyle(),
                    "video.overlay.caption.posX", "video.overlay.caption.posY", 0.05, 0.05);
            addPlaceholder("Case", Placeholder.Kind.SUBTITLE, service.getSubtitleStyle(),
                    "video.overlay.subtitle.posX", "video.overlay.subtitle.posY", 0.05, 0.75);
            addPlaceholder("Action", Placeholder.Kind.ACTION, service.getActionStyle(),
                    "video.overlay.action.posX", "video.overlay.action.posY", 0.75, 0.05);

            // Ruhigere Animation: langsamere Phase und weichere Bewegung
            visualizerTimer = new Timer(70, e -> {
                phase += 0.04f;
                repaint();
            });
            visualizerTimer.start();

            // Kontextmenü für Reset-auf-Standard
            JPopupMenu menu = new JPopupMenu();
            JMenuItem reset = new JMenuItem("Auf Standard zurücksetzen");
            reset.addActionListener(e -> resetToDefaults());
            menu.add(reset);
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { maybeShow(e); }
                @Override public void mouseReleased(MouseEvent e) { maybeShow(e); }
                private void maybeShow(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        menu.show(OverlayPreviewPanel.this, e.getX(), e.getY());
                    }
                }
            });
        }

        public void resetToDefaults() {
            // Sinnvolle Default-Styles mit gutem Kontrast und ~50% Transparenz
            VideoOverlayStyle caption = new VideoOverlayStyle("#FFFFFF", "rgba(0,0,0,0.50)", 26);
            VideoOverlayStyle subtitle = new VideoOverlayStyle("#FFFFFF", "rgba(0,0,0,0.50)", 20);
            VideoOverlayStyle action = new VideoOverlayStyle("#000000", "rgba(255,255,0,0.40)", 18);

            service.applyCaptionStyle(caption);
            service.applySubtitleStyle(subtitle);
            service.applyActionStyle(action);
            service.setCaptionEnabled(true);
            service.setSubtitleEnabled(true);
            service.setActionTransientEnabled(true);

            // Default-Positionen in Settings zurücksetzen (oben links, unten mittig, oben rechts)
            SettingsService.getInstance().set("video.overlay.caption.posX", 0.05d);
            SettingsService.getInstance().set("video.overlay.caption.posY", 0.05d);
            SettingsService.getInstance().set("video.overlay.subtitle.posX", 0.50d);
            SettingsService.getInstance().set("video.overlay.subtitle.posY", 0.85d);
            SettingsService.getInstance().set("video.overlay.action.posX", 0.75d);
            SettingsService.getInstance().set("video.overlay.action.posY", 0.05d);

            // Preview-Placeholder neu aus Styles befüllen
            for (Placeholder p : placeholders) {
                VideoOverlayStyle s = p.getCurrentStyle();
                p.applyStyle(s);
                // Position aktualisieren
                double px = getDouble(p.keyPosX, 0.05d);
                double py = getDouble(p.keyPosY, 0.05d);
                int w = p.getWidth();
                int h = p.getHeight();
                int x = (int) Math.round(px * (videoSize.width - w));
                int y = (int) Math.round(py * (videoSize.height - h));
                p.setLocation(x, y);
            }
            repaint();
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
            // zunächst hinzufügen, dann mit aktueller Größe positionieren
            add(p, JLayeredPane.PALETTE_LAYER);
            p.repositionFromPercent();
            placeholders.add(p);
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
        final String keyPosX;
        final String keyPosY;
        private Point dragOffset;
        private boolean disabled; // wenn Typ "Nicht verwenden" gewählt ist
        private String previewText;
        private Color previewFontColor;
        private Color previewBgBase; // ohne Alpha
        private int previewBgAlpha255 = 180; // 0=transparent, 255=deckend (für Füllung verwenden wir 255-alphaSlider in alter Logik)
        private int previewFontPx = 16;
        private Color previewBorderColor;
        private boolean roundedBorder = true;

        Placeholder(String label, Kind kind, VideoOverlayStyle style,
                    VideoOverlayService service, JComponent parent,
                    String keyPosX, String keyPosY) {
            this.kind = kind;
            this.service = service;
            this.keyPosX = keyPosX;
            this.keyPosY = keyPosY;
            this.previewText = "Beispieltext " + label;
            setLayout(null); // eigenes Painting übernimmt Layout
            setOpaque(false);
            applyStyle(style);

            // Dragging beibehalten
            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { dragOffset = e.getPoint(); }
                @Override public void mouseDragged(MouseEvent e) {
                    if (dragOffset == null || disabled) return;
                    int nx = getX() + e.getX() - dragOffset.x;
                    int ny = getY() + e.getY() - dragOffset.y;
                    setLocation(nx, ny);
                    Dimension size = parent.getSize();
                    double px = (double) nx / (double) Math.max(1, size.width - getWidth());
                    double py = (double) ny / (double) Math.max(1, size.height - getHeight());
                    SettingsService.getInstance().set(keyPosX, px);
                    SettingsService.getInstance().set(keyPosY, py);
                    parent.repaint();
                }
                @Override public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        openConfigDialog(parent);
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);

            // Kontextmenü: Einstellungen öffnen + Standard
            JPopupMenu pm = new JPopupMenu();
            JMenuItem miCfg = new JMenuItem("Einstellungen...");
            miCfg.addActionListener(ev -> openConfigDialog(parent));
            JMenuItem miReset = new JMenuItem("Auf Standard zurücksetzen");
            miReset.addActionListener(ev -> {
                java.awt.Component c = SwingUtilities.getAncestorOfClass(OverlayPreviewPanel.class, this);
                if (c instanceof OverlayPreviewPanel) ((OverlayPreviewPanel) c).resetToDefaults();
            });
            pm.add(miCfg); pm.add(miReset);
            addMouseListener(new MouseAdapter(){
                @Override public void mousePressed(MouseEvent e){ maybeShow(e);} @Override public void mouseReleased(MouseEvent e){ maybeShow(e);}
                private void maybeShow(MouseEvent e){ if (e.isPopupTrigger()) pm.show(Placeholder.this, e.getX(), e.getY()); }
            });

            // Initialgröße passend zum Text setzen
            updateSizeFromText();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (disabled) {
                    g2.setColor(new Color(200,200,200,120)); g2.fillRect(0,0,getWidth(),getHeight());
                    g2.setColor(new Color(120,120,120,200)); g2.drawRect(0,0,getWidth()-1,getHeight()-1);
                    g2.setColor(new Color(80,80,80,200));
                    String t = "(Nicht verwendet)";
                    FontMetrics fm = g2.getFontMetrics(getFont());
                    int tx = (getWidth()-fm.stringWidth(t))/2; int ty = (getHeight()+fm.getAscent()-fm.getDescent())/2;
                    g2.drawString(t, Math.max(4,tx), Math.max(fm.getAscent()+2,ty));
                    return;
                }
                // Box zeichnen
                int w = getWidth(); int h = getHeight();
                Color fill = new Color(previewBgBase.getRed(), previewBgBase.getGreen(), previewBgBase.getBlue(), Math.max(0, Math.min(255, previewBgAlpha255)));
                g2.setColor(fill);
                int arc = roundedBorder ? Math.min(24, Math.min(w,h)/4) : 0;
                if (arc > 0) g2.fillRoundRect(0,0,w,h, arc, arc); else g2.fillRect(0,0,w,h);
                // Text zentriert
                g2.setFont(new Font(getFont().getName(), Font.PLAIN, previewFontPx));
                g2.setColor(previewFontColor != null ? previewFontColor : Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                java.util.List<String> lines = wrapLines(previewText, fm, w - 16);
                int textH = 0; for (String ln : lines) textH += fm.getAscent()+fm.getDescent();
                int y = (h - textH)/2 + fm.getAscent();
                for (String ln : lines) {
                    int tw = fm.stringWidth(ln);
                    int x = (w - tw)/2;
                    g2.drawString(ln, Math.max(8,x), y);
                    y += fm.getAscent()+fm.getDescent();
                }
                // Rahmen
                g2.setColor(previewBorderColor != null ? previewBorderColor : (previewFontColor != null ? previewFontColor : Color.WHITE));
                if (arc > 0) g2.drawRoundRect(0,0,w-1,h-1, arc, arc); else g2.drawRect(0,0,w-1,h-1);
            } finally {
                g2.dispose();
            }
        }

        private java.util.List<String> wrapLines(String text, FontMetrics fm, int maxW) {
          java.util.List<String> out = new java.util.ArrayList<>();
          if (text == null) { out.add(""); return out; }
          for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim(); if (line.isEmpty()) { out.add(""); continue; }
            StringBuilder buf = new StringBuilder();
            for (String word : line.split("\\s+")) {
              if (buf.length()==0) buf.append(word);
              else {
                String trial = buf + " " + word;
                if (fm.stringWidth(trial) <= Math.max(32, maxW)) buf.append(" ").append(word);
                else { out.add(buf.toString()); buf.setLength(0); buf.append(word); }
              }
            }
            out.add(buf.toString());
          }
          return out;
        }

        private void updateSizeFromText() {
          Font f = new Font(getFont().getName(), Font.PLAIN, previewFontPx);
          BufferedImage tmp = new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
          Graphics2D g = tmp.createGraphics();
          try {
            FontMetrics fm = g.getFontMetrics(f);
            java.util.List<String> lines = wrapLines(previewText, fm, (int)Math.round(getParent() != null ? getParent().getWidth()*0.5 : 400));
            int textW = 0; int textH = 0;
            for (String ln : lines) { textW = Math.max(textW, fm.stringWidth(ln)); textH += fm.getAscent()+fm.getDescent(); }
            int pad = 16;
            int w = Math.max(80, textW + pad*2);
            int h = Math.max(40, textH + pad*2);
            setSize(w, h);
            setPreferredSize(new Dimension(w,h));
            revalidate(); repaint();
          } finally { g.dispose(); }
        }

        // Hilfsmethode: Position anhand gespeicherter Prozentwerte und aktueller Größe neu setzen
        void repositionFromPercent() {
            Component parent = getParent();
            if (parent == null) return;
            Dimension size = parent.getSize();
            Double px = SettingsService.getInstance().get(keyPosX, Double.class);
            Double py = SettingsService.getInstance().get(keyPosY, Double.class);
            if (px == null) px = 0.05d; if (py == null) py = 0.05d;
            int w = getWidth() > 0 ? getWidth() : getPreferredSize().width;
            int h = getHeight() > 0 ? getHeight() : getPreferredSize().height;
            int x = (int) Math.round(px * Math.max(1, (size.width - w)));
            int y = (int) Math.round(py * Math.max(1, (size.height - h)));
            setLocation(x, y);
        }

        void applyStyle(VideoOverlayStyle style) {
            if (style == null) return;
            try {
                previewFontColor = Color.decode(style.getFontColor());
            } catch (Exception ignore) { previewFontColor = Color.WHITE; }
            try {
                String bgStr = style.getBackgroundColor();
                if (bgStr != null && bgStr.toLowerCase().startsWith("rgba")) {
                    int a = bgStr.indexOf('('), b = bgStr.indexOf(')');
                    String[] p = bgStr.substring(a+1,b).split(",");
                    int r=Integer.parseInt(p[0].trim()), g=Integer.parseInt(p[1].trim()), bl=Integer.parseInt(p[2].trim());
                    float af = Float.parseFloat(p[3].trim()); // 0 deckend, 1 transparent
                    previewBgBase = new Color(r,g,bl);
                    previewBgAlpha255 = 255 - Math.round(255 * Math.max(0f, Math.min(1f, af)));
                } else {
                    Color c = Color.decode(style.getBackgroundColor()); previewBgBase = c; previewBgAlpha255 = 128;
                }
            } catch (Exception ignore) { previewBgBase = new Color(0,0,0); previewBgAlpha255 = 128; }
            previewFontPx = Math.max(8, style.getFontSizePx());
            updateSizeFromText();
        }

        private void openConfigDialog(Component parent) {
            VideoOverlayStyle current = getCurrentStyle();

            // Aktuelle Farben und Transparenz vorbereiten
            Color initialFont;
            Color initialBg;
            int initialAlphaSlider = 64; // 0 = deckend, 255 = voll transparent
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
                            // af = 0 => voll deckend, af = 1 => voll transparent
                            initialAlphaSlider = (int) Math.round(255 * Math.max(0f, Math.min(1f, af)));
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

            JButton borderColorPreview = new JButton();
            borderColorPreview.setPreferredSize(new Dimension(24, 24));
            borderColorPreview.setBackground(initialFont);
            borderColorPreview.setToolTipText("Rahmenfarbe auswählen");

            final Color[] chosenFont = { initialFont };
            final Color[] chosenBg = { initialBg };
            final Color[] chosenBorder = { initialFont };

            // Transparenz-Slider mit sofortiger Vorschau (0 = deckend, 255 = voll transparent)
            JSlider alphaSlider = new JSlider(0, 255, initialAlphaSlider);
            alphaSlider.setPaintTicks(true);
            alphaSlider.setPaintLabels(true);
            alphaSlider.setMajorTickSpacing(85);

            fontColorPreview.addActionListener(e -> {
                Color c = JColorChooser.showDialog(parent, "Textfarbe wählen", chosenFont[0]);
                if (c != null) {
                    chosenFont[0] = c;
                    fontColorPreview.setBackground(c);
                    previewFontColor = c;
                    repaint();
                }
            });
            bgColorPreview.addActionListener(e -> {
                Color c = JColorChooser.showDialog(parent, "Hintergrundfarbe wählen", chosenBg[0]);
                if (c != null) {
                    chosenBg[0] = c;
                    bgColorPreview.setBackground(c);
                    previewBgBase = c;
                    repaint();
                }
            });
            borderColorPreview.addActionListener(e -> {
                Color c = JColorChooser.showDialog(parent, "Rahmenfarbe wählen", chosenBorder[0]);
                if (c != null) {
                    chosenBorder[0] = c;
                    borderColorPreview.setBackground(c);
                    previewBorderColor = c;
                    repaint();
                }
            });

            alphaSlider.addChangeListener(e -> {
                int sliderVal = alphaSlider.getValue();
                previewBgAlpha255 = 255 - sliderVal; // hoher Slider => transparenter Hintergrund
                repaint();
            });

            JSpinner spFontSize = new JSpinner(new SpinnerNumberModel(current.getFontSizePx(), 8, 96, 1));
            spFontSize.addChangeListener(e -> {
                int fs = (Integer) spFontSize.getValue();
                previewFontPx = Math.max(8, fs);
                updateSizeFromText();
                repositionFromPercent();
            });

            String[] borderStyles = {"Gerader Rahmen", "Abgerundeter Rahmen", "Kein Rahmen"};
            JComboBox<String> cbBorderStyle = new JComboBox<>(borderStyles);
            cbBorderStyle.setSelectedIndex(1);
            cbBorderStyle.addActionListener(e -> {
                String sel = (String) cbBorderStyle.getSelectedItem();
                if ("Kein Rahmen".equals(sel)) {
                    previewBorderColor = new Color(0,0,0,0); // unsichtbar
                } else {
                    if (previewBorderColor == null) previewBorderColor = previewFontColor;
                }
                roundedBorder = "Abgerundeter Rahmen".equals(sel);
                repaint();
            });

            // Dauer-Slider inkl. ∞
            int currentDuration = (kind == Kind.ACTION) ? service.getActionTransientDurationMs() :
                    (kind == Kind.CAPTION ? service.getSuiteDisplayDurationMs() : service.getCaseDisplayDurationMs());
            int minMs = 250; final int specialMaxValue = 11_000;
            int sliderInit = (currentDuration >= specialMaxValue) ? specialMaxValue : Math.max(minMs, Math.min(10_000, currentDuration));
            JSlider durationSlider = new JSlider(minMs, specialMaxValue, sliderInit);
            durationSlider.setPaintTicks(true); durationSlider.setMajorTickSpacing(2500); durationSlider.setMinorTickSpacing(250);
            JLabel durationValueLabel = new JLabel();
            Runnable updateDurationLabel = () -> durationValueLabel.setText(durationSlider.getValue() >= specialMaxValue ? "∞" : durationSlider.getValue() + " ms");
            durationSlider.addChangeListener(e -> updateDurationLabel.run());
            updateDurationLabel.run();

            // Aktiviert-Checkbox (statt Typ-Dropdown)
            final JCheckBox enabledCheck = new JCheckBox("Aktiviert");
            boolean initiallyEnabled;
            if (kind == Kind.CAPTION) initiallyEnabled = service.isCaptionEnabled();
            else if (kind == Kind.SUBTITLE) initiallyEnabled = service.isSubtitleEnabled();
            else initiallyEnabled = service.isActionTransientEnabled();
            enabledCheck.setSelected(initiallyEnabled);

            // Panel layout
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(4,4,4,4);
            c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST; panel.add(new JLabel("Aktiviert:"), c);
            c.gridx = 1; panel.add(enabledCheck, c);

            c.gridx = 0; c.gridy = 1; panel.add(new JLabel("Textfarbe:"), c);
            c.gridx = 1; panel.add(fontColorPreview, c);

            c.gridx = 0; c.gridy = 2; panel.add(new JLabel("Hintergrundfarbe:"), c);
            c.gridx = 1; panel.add(bgColorPreview, c);

            c.gridx = 0; c.gridy = 3; panel.add(new JLabel("Hintergrund-Transparenz:"), c);
            c.gridx = 1; panel.add(alphaSlider, c);

            c.gridx = 0; c.gridy = 4; panel.add(new JLabel("Rahmenfarbe:"), c);
            c.gridx = 1; panel.add(borderColorPreview, c);

            c.gridx = 0; c.gridy = 5; panel.add(new JLabel("Rahmendesign:"), c);
            c.gridx = 1; panel.add(cbBorderStyle, c);

            c.gridx = 0; c.gridy = 6; panel.add(new JLabel("Schriftgröße (px):"), c);
            c.gridx = 1; panel.add(spFontSize, c);

            c.gridx = 0; c.gridy = 7; panel.add(new JLabel("Anzeigedauer:"), c);
            JPanel durationPanel = new JPanel(new BorderLayout(4,0));
            durationPanel.add(durationSlider, BorderLayout.CENTER);
            durationPanel.add(durationValueLabel, BorderLayout.EAST);
            c.gridx = 1; panel.add(durationPanel, c);

            int res = JOptionPane.showConfirmDialog(parent, panel,
                    "Overlay-Platzhalter konfigurieren", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res == JOptionPane.OK_OPTION) {
                // Farben ermitteln
                Color f = chosenFont[0]; Color b = chosenBg[0]; Color borderColor = chosenBorder[0];
                int sliderVal = alphaSlider.getValue(); double alphaFactor = Math.max(0d, Math.min(1d, sliderVal / 255d));
                if (f == null || b == null) return;
                String fontHex = String.format("#%02X%02X%02X", f.getRed(), f.getGreen(), f.getBlue());
                String bgRgba = String.format("rgba(%d,%d,%d,%.3f)", b.getRed(), b.getGreen(), b.getBlue(), alphaFactor);
                int fontPx = (Integer) spFontSize.getValue();
                VideoOverlayStyle newStyle = new VideoOverlayStyle(fontHex, bgRgba, fontPx);

                // Rahmen setzen
                if ("Kein Rahmen".equals(cbBorderStyle.getSelectedItem())) {
                    setBorder(BorderFactory.createEmptyBorder());
                } else {
                    Color useBorder = borderColor != null ? borderColor : f;
                    boolean rounded = "Abgerundeter Rahmen".equals(cbBorderStyle.getSelectedItem());
                    setBorder(BorderFactory.createLineBorder(useBorder, 1, rounded));
                }

                // Aktiviert-Flag speichern
                boolean enabled = enabledCheck.isSelected();
                if (kind == Kind.CAPTION) service.setCaptionEnabled(enabled);
                else if (kind == Kind.SUBTITLE) service.setSubtitleEnabled(enabled);
                else service.setActionTransientEnabled(enabled);

                // Dauer speichern
                int v = durationSlider.getValue();
                int durationToStore = (v >= specialMaxValue) ? specialMaxValue : v;
                if (kind == Kind.CAPTION) service.setSuiteDisplayDurationMs(durationToStore);
                else if (kind == Kind.SUBTITLE) service.setCaseDisplayDurationMs(durationToStore);
                else service.setActionTransientDurationMs(durationToStore);

                // Style anwenden
                if (enabled) {
                    applyStyle(newStyle);
                    previewBorderColor = chosenBorder[0] != null ? chosenBorder[0] : chosenFont[0];
                    roundedBorder = !"Gerader Rahmen".equals(cbBorderStyle.getSelectedItem()) && !"Kein Rahmen".equals(cbBorderStyle.getSelectedItem());
                    updateSizeFromText();
                    repositionFromPercent();
                    applyToService(kind, newStyle);
                } else {
                    disabled = true; repaint();
                }
            }
        }

        VideoOverlayStyle getCurrentStyle() {
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
