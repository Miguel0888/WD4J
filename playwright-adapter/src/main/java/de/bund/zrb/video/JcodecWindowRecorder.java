package de.bund.zrb.video;

import com.sun.jna.platform.win32.WinDef;
import de.bund.zrb.config.VideoConfig;
import de.bund.zrb.win.WindowCapture;
import org.jcodec.api.awt.AWTSequenceEncoder;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Pure Java window recorder using JCodec (no audio). */
public class JcodecWindowRecorder implements Closeable {
    private final WinDef.HWND hWnd;
    private final Path outFileRequested;
    private final int fpsArg;

    private AWTSequenceEncoder encoder;
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private Path outFileEffective;

    // Overlays – kompatible API zu WindowRecorder
    private final CaptionOverlay caption = new CaptionOverlay();
    private final SubtitleOverlay subtitle = new SubtitleOverlay();

    public JcodecWindowRecorder(WinDef.HWND hWnd, Path outFile, int fps) {
        this.hWnd = hWnd;
        this.outFileRequested = outFile;
        this.fpsArg = fps;
    }

    private static volatile JcodecWindowRecorder CURRENT;
    public static JcodecWindowRecorder getCurrentActive() { return CURRENT; }

    // Overlay-API
    public void setCaptionText(String text) { caption.setText(text); }
    public void setCaptionVisible(boolean visible) { caption.setVisible(visible); }
    public void setCaptionStyle(WindowRecorder.OverlayStyle style) { caption.setStyle(style); }
    public void setSubtitleText(String text) { subtitle.setText(text); }
    public void setSubtitleVisible(boolean visible) { subtitle.setVisible(visible); }
    public void setSubtitleStyle(WindowRecorder.OverlayStyle style) { subtitle.setStyle(style); }

    public void start() throws Exception {
        CURRENT = this;
        if (running.get()) return;

        BufferedImage probe = WindowCapture.capture(hWnd);
        if (probe == null) throw new IllegalStateException("WindowCapture liefert null");

        int w = probe.getWidth();
        int h = probe.getHeight();
        if (VideoConfig.isEnforceEvenDims()) { w = makeEven(w); h = makeEven(h); }

        int fps = (fpsArg > 0) ? fpsArg : VideoConfig.getFps();
        if (fps <= 0) fps = 15;

        outFileEffective = outFileRequested;

        // AWTSequenceEncoder mit fixer FPS
        encoder = AWTSequenceEncoder.createSequenceEncoder(new File(outFileEffective.toString()), fps);

        final int frameIntervalMs = (int) Math.max(1, Math.round(1000.0 / Math.max(1, fps)));

        running.set(true);
        worker = new Thread(() -> {
            while (running.get()) {
                try {
                    BufferedImage img = WindowCapture.capture(hWnd);
                    if (img != null) {
                        int iw = img.getWidth(), ih = img.getHeight();
                        int tw = iw, th = ih;
                        if (VideoConfig.isEnforceEvenDims()) { tw = makeEven(iw); th = makeEven(ih); }
                        BufferedImage frame;
                        if (tw != iw || th != ih) {
                            frame = new BufferedImage(tw, th, BufferedImage.TYPE_3BYTE_BGR);
                            Graphics2D g = frame.createGraphics();
                            try { g.drawImage(img, 0, 0, tw, th, null); applyOverlays(frame); } finally { g.dispose(); }
                        } else {
                            if (img.getType() != BufferedImage.TYPE_3BYTE_BGR) {
                                frame = new BufferedImage(iw, ih, BufferedImage.TYPE_3BYTE_BGR);
                                Graphics2D g = frame.createGraphics();
                                try { g.drawImage(img, 0, 0, null); } finally { g.dispose(); }
                            } else {
                                frame = img;
                            }
                            applyOverlays(frame);
                        }
                        encoder.encodeImage(frame);
                    }
                    Thread.sleep(frameIntervalMs);
                } catch (Throwable t) {
                    running.set(false);
                }
            }
        }, "jcodec-window-recorder");
        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        running.set(false);
        if (worker != null) { try { worker.join(2000); } catch (InterruptedException ignored) {} }
        try { if (encoder != null) encoder.finish(); } catch (Exception ignored) {}
        if (CURRENT == this) CURRENT = null;
    }

    @Override public void close() { stop(); }

    public Path getEffectiveOutput() { return outFileEffective != null ? outFileEffective : outFileRequested; }

    private static int makeEven(int v) { return (v & 1) == 1 ? v - 1 : v; }

    private void applyOverlays(BufferedImage frame) {
        caption.paint(frame);
        subtitle.paint(frame);
    }

    // ===== Overlay-Implementation (kopiert aus WindowRecorder, kompatibel) =====

    private static void paintOverlay(BufferedImage frame, String text, WindowRecorder.OverlayStyle st) {
        if (text == null || text.isEmpty() || st == null) return;
        Graphics2D g = (Graphics2D) frame.getGraphics();
        try { g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); } catch (Throwable ignore) {}
        try {
            g.setFont(new Font(st.fontName, st.fontStyle, st.fontSizePt));
            FontMetrics fm = g.getFontMetrics();
            int maxW = (int) Math.max(60, frame.getWidth() * st.maxWidthRatio);
            List<String> lines = wrapLines(text, fm, maxW);
            int textW = 0; int textH = 0;
            for (String ln : lines) { textW = Math.max(textW, fm.stringWidth(ln)); textH += fm.getAscent() + fm.getDescent(); }
            textH += st.lineGapPx * Math.max(0, lines.size() - 1);
            int boxW = textW + 2 * st.paddingPx;
            int boxH = textH + 2 * st.paddingPx;
            int x;
            if (st.hAlign == WindowRecorder.OverlayStyle.HAlign.LEFT) x = st.marginX;
            else if (st.hAlign == WindowRecorder.OverlayStyle.HAlign.RIGHT) x = frame.getWidth() - st.marginX - boxW;
            else x = (frame.getWidth() - boxW) / 2;
            int y = (st.vAnchor == WindowRecorder.OverlayStyle.VAnchor.TOP)
                    ? st.marginY
                    : frame.getHeight() - st.marginY - boxH;
            Composite old = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, clamp01(st.boxAlpha)));
            g.setColor(st.boxColor);
            g.fillRoundRect(x, y, boxW, boxH, st.cornerRadius, st.cornerRadius);
            g.setComposite(old);
            int tx = x + st.paddingPx;
            int ty = y + st.paddingPx + fm.getAscent();
            if (st.textShadow) {
                g.setColor(new Color(0, 0, 0, 200));
                for (String ln : lines) { g.drawString(ln, tx + 1, ty + 1); ty += fm.getAscent() + fm.getDescent() + st.lineGapPx; }
                ty = y + st.paddingPx + fm.getAscent();
            }
            g.setColor(st.textColor);
            for (String ln : lines) { g.drawString(ln, tx, ty); ty += fm.getAscent() + fm.getDescent() + st.lineGapPx; }
        } finally { g.dispose(); }
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }

    private static List<String> wrapLines(String text, FontMetrics fm, int maxWidth) {
        List<String> out = new ArrayList<>();
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) { out.add(""); continue; }
            StringBuilder buf = new StringBuilder();
            for (String word : line.split("\\s+")) {
                if (buf.length() == 0) { buf.append(word); }
                else {
                    String trial = buf + " " + word;
                    if (fm.stringWidth(trial) <= maxWidth) { buf.append(" ").append(word); }
                    else { out.add(buf.toString()); buf.setLength(0); buf.append(word); }
                }
            }
            out.add(buf.toString());
        }
        return out;
    }

    private static final class CaptionOverlay {
        private String text = "";
        private boolean visible = true;
        private WindowRecorder.OverlayStyle style = WindowRecorder.OverlayStyle.defaultCaption();
        void setText(String t) { this.text = t == null ? "" : t; }
        void setVisible(boolean v) { this.visible = v; }
        void setStyle(WindowRecorder.OverlayStyle s) { if (s != null) this.style = s; }
        void paint(BufferedImage frame) { if (visible) paintOverlay(frame, text, style); }
    }
    private static final class SubtitleOverlay {
        private String text = "";
        private boolean visible = false;
        private WindowRecorder.OverlayStyle style = WindowRecorder.OverlayStyle.defaultSubtitle();
        void setText(String t) { this.text = t == null ? "" : t; }
        void setVisible(boolean v) { this.visible = v; }
        void setStyle(WindowRecorder.OverlayStyle s) { if (s != null) this.style = s; }
        void paint(BufferedImage frame) { if (visible) paintOverlay(frame, text, style); }
    }
}
