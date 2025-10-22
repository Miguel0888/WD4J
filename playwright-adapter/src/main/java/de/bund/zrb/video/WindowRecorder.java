package de.bund.zrb.video;

import com.sun.jna.platform.win32.WinDef;
import de.bund.zrb.config.VideoConfig;
import de.bund.zrb.win.WindowCapture;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class WindowRecorder implements Closeable {
    private final WinDef.HWND hWnd;
    private final Path outFileRequested;
    private final int fpsArg;

    private FFmpegFrameRecorder rec;
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Java2DFrameConverter conv = new Java2DFrameConverter();

    private Path outFileEffective;

    // --- NEU: zwei Overlays ---
    private final CaptionOverlay caption = new CaptionOverlay();
    private final SubtitleOverlay subtitle = new SubtitleOverlay();

    public WindowRecorder(WinDef.HWND hWnd, Path outFile, int fps) {
        this.hWnd = hWnd;
        this.outFileRequested = outFile;
        this.fpsArg = fps;
    }

    private static volatile WindowRecorder CURRENT;
    public static WindowRecorder getCurrentActive() { return CURRENT; }

    // -------- Public API: Caption (oben) --------
    public void setCaptionText(String text) { caption.setText(text); }
    public void setCaptionVisible(boolean visible) { caption.setVisible(visible); }
    public void setCaptionStyle(OverlayStyle style) { caption.setStyle(style); }

    // -------- Public API: Subtitle (unten) ------
    public void setSubtitleText(String text) { subtitle.setText(text); }
    public void setSubtitleVisible(boolean visible) { subtitle.setVisible(visible); }
    public void setSubtitleStyle(OverlayStyle style) { subtitle.setStyle(style); }

    public void start() throws Exception {
        CURRENT = this;
        if (running.get()) return;

        BufferedImage probe = WindowCapture.capture(hWnd);
        if (probe == null) throw new IllegalStateException("WindowCapture liefert null");

        int w = probe.getWidth();
        int h = probe.getHeight();
        if (VideoConfig.isEnforceEvenDims()) {
            w = makeEven(w);
            h = makeEven(h);
        }
        final int[] dim = new int[] { w, h };

        final int fps = (fpsArg > 0) ? fpsArg : VideoConfig.getFps();
        final long frameIntervalMs = Math.max(1, Math.round(1000.0 / Math.max(1, fps)));

        // 1) Primär-Container/Format aus Config
        String container = VideoConfig.getContainer();
        String primaryExt = VideoConfig.mapContainerToExt(container);
        outFileEffective = ensureExtension(outFileRequested, primaryExt);

        rec = new FFmpegFrameRecorder(outFileEffective.toFile(), dim[0], dim[1]);
        rec.setFormat(container);
        VideoConfig.configureRecorder(rec, fps);

        try {
            rec.start();
        } catch (Throwable primaryFail) {
            safeRelease(rec);
            // 2) Fallback-Reihenfolge probieren
            List<String> fallbacks = VideoConfig.getContainerFallbacks();
            boolean started = false;
            Throwable lastEx = primaryFail;

            for (String fb : fallbacks) {
                try {
                    String ext = VideoConfig.mapContainerToExt(fb);
                    outFileEffective = ensureExtension(outFileRequested, ext);
                    rec = new FFmpegFrameRecorder(outFileEffective.toFile(), dim[0], dim[1]);
                    rec.setFormat(fb);
                    VideoConfig.configureRecorder(rec, fps);
                    rec.start();
                    started = true;
                    break;
                } catch (Throwable ex) {
                    safeRelease(rec);
                    lastEx = ex;
                }
            }
            if (!started) {
                throw new IllegalStateException("Konnte Recorder nicht starten (Primär+Fallbacks). Letzte Ursache: " + lastEx, lastEx);
            }
        }

        running.set(true);
        worker = new Thread(() -> {
            while (running.get()) {
                try {
                    BufferedImage img = WindowCapture.capture(hWnd);
                    if (img != null) {
                        img = toBGR(img);
                        int iw = img.getWidth(), ih = img.getHeight();

                        int tw = iw, th = ih;
                        if (VideoConfig.isEnforceEvenDims()) {
                            tw = makeEven(iw);
                            th = makeEven(ih);
                        }

                        if (tw != dim[0] || th != dim[1]) {
                            dim[0] = tw; dim[1] = th;
                            BufferedImage scaled = new BufferedImage(dim[0], dim[1], BufferedImage.TYPE_3BYTE_BGR);
                            Graphics2D g = scaled.createGraphics();
                            try {
                                g.drawImage(img, 0, 0, dim[0], dim[1], null);
                                // --- NEU: Overlays nach dem Skalieren zeichnen ---
                                applyOverlays(scaled);
                            } finally {
                                g.dispose();
                            }
                            rec.record(conv.convert(scaled));
                        } else {
                            // --- NEU: Overlays direkt zeichnen ---
                            applyOverlays(img);
                            rec.record(conv.convert(img));
                        }
                    }
                    Thread.sleep(frameIntervalMs);
                } catch (Throwable t) {
                    running.set(false);
                }
            }
        }, "window-recorder");
        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        running.set(false);
        if (worker != null) {
            try { worker.join(2000); } catch (InterruptedException ignored) {}
        }
        try { if (rec != null) rec.stop(); } catch (Exception ignored) {}
        try { if (rec != null) rec.release(); } catch (Exception ignored) {}
        if (CURRENT == this) CURRENT = null;
    }

    @Override public void close() { stop(); }

    public Path getEffectiveOutput() {
        return outFileEffective != null ? outFileEffective : outFileRequested;
    }

    private static int makeEven(int v) { return (v & 1) == 1 ? v - 1 : v; }

    private static void safeRelease(FFmpegFrameRecorder r) {
        try { if (r != null) r.stop(); } catch (Exception ignored) {}
        try { if (r != null) r.release(); } catch (Exception ignored) {}
    }

    private static Path ensureExtension(Path p, String ext) {
        String s = p.toString();
        int dot = s.lastIndexOf('.');
        if (dot > 0) s = s.substring(0, dot);
        return Paths.get(s + ext);
    }

    /** Erzwingt TYPE_3BYTE_BGR (JavaCV/FFmpeg vermeidet so Kanalmapping-Irritationen). */
    private static BufferedImage toBGR(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_3BYTE_BGR) return src;
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        Graphics2D g = dst.createGraphics();
        try { g.drawImage(src, 0, 0, null); } finally { g.dispose(); }
        return dst;
    }

    // =========================================================
    // ===============   Overlay-Unterbau   ====================
    // =========================================================

    private void applyOverlays(BufferedImage frame) {
        caption.paint(frame);
        subtitle.paint(frame);
    }

    /** Stil für beide Overlays. */
    public static final class OverlayStyle {
        public enum HAlign { LEFT, CENTER, RIGHT }
        public enum VAnchor { TOP, BOTTOM }

        public final String fontName;
        public final int fontStyle;
        public final int fontSizePt;
        public final Color textColor;
        public final boolean textShadow;
        public final Color boxColor;
        public final float boxAlpha;
        public final int paddingPx;
        public final int cornerRadius;
        public final int marginX;
        public final int marginY;
        public final double maxWidthRatio;
        public final int lineGapPx;
        public final HAlign hAlign;
        public final VAnchor vAnchor;

        private OverlayStyle(String fontName, int fontStyle, int fontSizePt,
                             Color textColor, boolean textShadow,
                             Color boxColor, float boxAlpha,
                             int paddingPx, int cornerRadius,
                             int marginX, int marginY,
                             double maxWidthRatio, int lineGapPx,
                             HAlign hAlign, VAnchor vAnchor) {
            this.fontName = fontName;
            this.fontStyle = fontStyle;
            this.fontSizePt = fontSizePt;
            this.textColor = textColor;
            this.textShadow = textShadow;
            this.boxColor = boxColor;
            this.boxAlpha = boxAlpha;
            this.paddingPx = paddingPx;
            this.cornerRadius = cornerRadius;
            this.marginX = marginX;
            this.marginY = marginY;
            this.maxWidthRatio = maxWidthRatio;
            this.lineGapPx = lineGapPx;
            this.hAlign = hAlign;
            this.vAnchor = vAnchor;
        }

        public static OverlayStyle defaultCaption() {
            // Oben zentriert, etwas größere Schrift
            return new OverlayStyle(
                    "SansSerif", Font.BOLD, 18,
                    Color.WHITE, true,
                    Color.BLACK, 0.50f,
                    10, 12,
                    12, 12,
                    0.80, 4,
                    HAlign.CENTER, VAnchor.TOP
            );
        }
        public static OverlayStyle defaultSubtitle() {
            // Unten zentriert
            return new OverlayStyle(
                    "SansSerif", Font.BOLD, 14,
                    Color.WHITE, true,
                    Color.BLACK, 0.55f,
                    8, 10,
                    12, 12,
                    0.80, 4,
                    HAlign.CENTER, VAnchor.BOTTOM
            );
        }

        public static class Builder {
            private OverlayStyle s;
            public Builder(OverlayStyle base) { this.s = base; }
            public Builder font(String name, int style, int pt){ s = new OverlayStyle(name, style, pt,
                    s.textColor, s.textShadow, s.boxColor, s.boxAlpha, s.paddingPx, s.cornerRadius,
                    s.marginX, s.marginY, s.maxWidthRatio, s.lineGapPx, s.hAlign, s.vAnchor); return this; }
            public Builder text(Color c, boolean shadow){ s = new OverlayStyle(s.fontName, s.fontStyle, s.fontSizePt,
                    c, shadow, s.boxColor, s.boxAlpha, s.paddingPx, s.cornerRadius, s.marginX, s.marginY,
                    s.maxWidthRatio, s.lineGapPx, s.hAlign, s.vAnchor); return this; }
            public Builder box(Color c, float alpha){ s = new OverlayStyle(s.fontName, s.fontStyle, s.fontSizePt,
                    s.textColor, s.textShadow, c, alpha, s.paddingPx, s.cornerRadius, s.marginX, s.marginY,
                    s.maxWidthRatio, s.lineGapPx, s.hAlign, s.vAnchor); return this; }
            public Builder padding(int px){ s = new OverlayStyle(s.fontName, s.fontStyle, s.fontSizePt,
                    s.textColor, s.textShadow, s.boxColor, s.boxAlpha, px, s.cornerRadius, s.marginX, s.marginY,
                    s.maxWidthRatio, s.lineGapPx, s.hAlign, s.vAnchor); return this; }
            public Builder radius(int r){ s = new OverlayStyle(s.fontName, s.fontStyle, s.fontSizePt,
                    s.textColor, s.textShadow, s.boxColor, s.boxAlpha, s.paddingPx, r, s.marginX, s.marginY,
                    s.maxWidthRatio, s.lineGapPx, s.hAlign, s.vAnchor); return this; }
            public Builder margin(int x, int y){ s = new OverlayStyle(s.fontName, s.fontStyle, s.fontSizePt,
                    s.textColor, s.textShadow, s.boxColor, s.boxAlpha, s.paddingPx, s.cornerRadius,
                    x, y, s.maxWidthRatio, s.lineGapPx, s.hAlign, s.vAnchor); return this; }
            public Builder maxWidth(double ratio){ s = new OverlayStyle(s.fontName, s.fontStyle, s.fontSizePt,
                    s.textColor, s.textShadow, s.boxColor, s.boxAlpha, s.paddingPx, s.cornerRadius, s.marginX, s.marginY,
                    ratio, s.lineGapPx, s.hAlign, s.vAnchor); return this; }
            public Builder lineGap(int px){ s = new OverlayStyle(s.fontName, s.fontStyle, s.fontSizePt,
                    s.textColor, s.textShadow, s.boxColor, s.boxAlpha, s.paddingPx, s.cornerRadius, s.marginX, s.marginY,
                    s.maxWidthRatio, px, s.hAlign, s.vAnchor); return this; }
            public Builder align(HAlign h, VAnchor v){ s = new OverlayStyle(s.fontName, s.fontStyle, s.fontSizePt,
                    s.textColor, s.textShadow, s.boxColor, s.boxAlpha, s.paddingPx, s.cornerRadius, s.marginX, s.marginY,
                    s.maxWidthRatio, s.lineGapPx, h, v); return this; }
            public OverlayStyle build() { return s; }
        }
    }

    /** Gemeinsamer Code zum Zeichnen. */
    private static void paintOverlay(BufferedImage frame, String text, OverlayStyle st) {
        if (text == null || text.isEmpty() || st == null) return;

        Graphics2D g = (Graphics2D) frame.getGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } catch (Throwable ignore) {}
        try {
            g.setFont(new Font(st.fontName, st.fontStyle, st.fontSizePt));
            FontMetrics fm = g.getFontMetrics();

            int maxW = (int) Math.max(60, frame.getWidth() * st.maxWidthRatio);
            java.util.List<String> lines = wrapLines(text, fm, maxW);

            int textW = 0;
            int textH = 0;
            for (String ln : lines) {
                textW = Math.max(textW, fm.stringWidth(ln));
                textH += fm.getAscent() + fm.getDescent();
            }
            textH += st.lineGapPx * Math.max(0, lines.size() - 1);

            int boxW = textW + 2 * st.paddingPx;
            int boxH = textH + 2 * st.paddingPx;

            int x;
            if (st.hAlign == OverlayStyle.HAlign.LEFT) {
                x = st.marginX;
            } else if (st.hAlign == OverlayStyle.HAlign.RIGHT) {
                x = frame.getWidth() - st.marginX - boxW;
            } else { // CENTER
                x = (frame.getWidth() - boxW) / 2;
            }

            int y;
            if (st.vAnchor == OverlayStyle.VAnchor.TOP) {
                y = st.marginY;
            } else { // BOTTOM
                y = frame.getHeight() - st.marginY - boxH;
            }

            // Box
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp01(st.boxAlpha)));
            g.setColor(st.boxColor);
            g.fillRoundRect(x, y, boxW, boxH, st.cornerRadius, st.cornerRadius);
            g.setComposite(old);

            // Text (optional Schatten)
            int tx = x + st.paddingPx;
            int ty = y + st.paddingPx + fm.getAscent();

            if (st.textShadow) {
                g.setColor(new Color(0, 0, 0, 200));
                for (String ln : lines) {
                    g.drawString(ln, tx + 1, ty + 1);
                    ty += fm.getAscent() + fm.getDescent() + st.lineGapPx;
                }
                ty = y + st.paddingPx + fm.getAscent();
            }

            g.setColor(st.textColor);
            for (String ln : lines) {
                g.drawString(ln, tx, ty);
                ty += fm.getAscent() + fm.getDescent() + st.lineGapPx;
            }
        } finally {
            g.dispose();
        }
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }

    private static java.util.List<String> wrapLines(String text, FontMetrics fm, int maxWidth) {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) { out.add(""); continue; }
            StringBuilder buf = new StringBuilder();
            for (String word : line.split("\\s+")) {
                if (buf.length() == 0) {
                    buf.append(word);
                } else {
                    String trial = buf + " " + word;
                    if (fm.stringWidth(trial) <= maxWidth) {
                        buf.append(" ").append(word);
                    } else {
                        out.add(buf.toString());
                        buf.setLength(0);
                        buf.append(word);
                    }
                }
            }
            out.add(buf.toString());
        }
        return out;
    }

    // =========================================================
    // ==============   Konkrete Overlays   ====================
    // =========================================================

    /** Oben (Kapitel/Titel). */
    public static final class CaptionOverlay {
        private final AtomicReference<String> text = new AtomicReference<>("");
        private volatile boolean visible = false;
        private volatile OverlayStyle style = OverlayStyle.defaultCaption();

        public void setText(String t) { text.set(t == null ? "" : t); }
        public void setVisible(boolean v) { visible = v; }
        public void setStyle(OverlayStyle s) { if (s != null) style = s; }

        private void paint(BufferedImage frame) {
            if (!visible) return;
            String t = text.get();
            if (t == null || t.isEmpty()) return;
            paintOverlay(frame, t, style);
        }
    }

    /** Unten (aktueller Schritt). */
    public static final class SubtitleOverlay {
        private final AtomicReference<String> text = new AtomicReference<>("");
        private volatile boolean visible = false;
        private volatile OverlayStyle style = OverlayStyle.defaultSubtitle();

        public void setText(String t) { text.set(t == null ? "" : t); }
        public void setVisible(boolean v) { visible = v; }
        public void setStyle(OverlayStyle s) { if (s != null) style = s; }

        private void paint(BufferedImage frame) {
            if (!visible) return;
            String t = text.get();
            if (t == null || t.isEmpty()) return;
            paintOverlay(frame, t, style);
        }
    }
}
