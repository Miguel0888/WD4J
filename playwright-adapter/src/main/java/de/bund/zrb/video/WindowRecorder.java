package de.bund.zrb.video;

import com.sun.jna.platform.win32.WinDef;
import de.bund.zrb.config.VideoConfig;
import de.bund.zrb.win.WindowCapture;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class WindowRecorder implements Closeable {
    private final WinDef.HWND hWnd;
    private final Path outFileRequested;
    private final int fpsArg;

    private FFmpegFrameRecorder rec;
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Java2DFrameConverter conv = new Java2DFrameConverter();

    private Path outFileEffective;

    public WindowRecorder(WinDef.HWND hWnd, Path outFile, int fps) {
        this.hWnd = hWnd;
        this.outFileRequested = outFile;
        this.fpsArg = fps;
    }

    public void start() throws Exception {
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
                        img = toBGR(img); // stabile RGB/BGR-Interpretation
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
                            try { g.drawImage(img, 0, 0, dim[0], dim[1], null); } finally { g.dispose(); }
                            rec.record(conv.convert(scaled));
                        } else {
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
}
