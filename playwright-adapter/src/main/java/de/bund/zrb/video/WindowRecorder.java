package de.bund.zrb.video;

import com.sun.jna.platform.win32.WinDef;
import de.bund.zrb.win.WindowCapture;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

public class WindowRecorder implements Closeable {
    private final WinDef.HWND hWnd;
    private final Path outFileRequested;
    private final int fps;

    private FFmpegFrameRecorder rec;
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Java2DFrameConverter conv = new Java2DFrameConverter();

    private Path outFileEffective;

    public WindowRecorder(WinDef.HWND hWnd, Path outFile, int fps) {
        this.hWnd = hWnd;
        this.outFileRequested = outFile;
        this.fps = fps <= 0 ? 15 : fps;
    }

    public void start() throws Exception {
        if (running.get()) return;

        BufferedImage probe = WindowCapture.capture(hWnd);
        if (probe == null) throw new IllegalStateException("WindowCapture liefert null");

        final int[] dim = new int[] { makeEven(probe.getWidth()), makeEven(probe.getHeight()) };
        final long frameIntervalMs = Math.max(1, Math.round(1000.0 / fps));

        // Primär: MKV + MJPEG (tolerant), aber mit YUV420P (kein YUVJ) und Full-Range
        outFileEffective = ensureExtension(outFileRequested, ".mkv");
        rec = new FFmpegFrameRecorder(outFileEffective.toFile(), dim[0], dim[1]);
        rec.setFormat("matroska");
        rec.setVideoCodec(avcodec.AV_CODEC_ID_MJPEG);
        rec.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);              // statt YUVJ420P
        rec.setFrameRate(fps);
        rec.setInterleaved(true);
        // Qualität/Bitrate (MJPEG nutzt qscale):
        rec.setVideoOption("qscale", "3");                          // 1=sehr gut, 31=schlecht

        // --- Farbraum/-range sauber setzen ---
        // Full range (PC) und BT.709 hilft gegen Cyan-/Waschout-Effekt
        rec.setVideoOption("color_range", "pc");                    // Full range (statt "tv")
        rec.setVideoOption("colorspace", "bt709");
        rec.setVideoOption("color_trc", "bt709");
        rec.setVideoOption("color_primaries", "bt709");
        // Fallback-Filter auf der sicheren Seite: erzwinge Range + Format
        rec.setVideoOption("vf", "scale=in_range=pc:out_range=pc,format=yuv420p");

        try {
            rec.start();
        } catch (Throwable mkvFail) {
            safeRelease(rec);
            // Fallback: AVI + MJPEG mit denselben Pixel-/Range-Optionen
            outFileEffective = ensureExtension(outFileRequested, ".avi");
            rec = new FFmpegFrameRecorder(outFileEffective.toFile(), dim[0], dim[1]);
            rec.setFormat("avi");
            rec.setVideoCodec(avcodec.AV_CODEC_ID_MJPEG);
            rec.setPixelFormat(avutil.AV_PIX_FMT_YUV420P);
            rec.setFrameRate(fps);
            rec.setInterleaved(true);
            rec.setVideoOption("qscale", "3");
            rec.setVideoOption("color_range", "pc");
            rec.setVideoOption("colorspace", "bt709");
            rec.setVideoOption("color_trc", "bt709");
            rec.setVideoOption("color_primaries", "bt709");
            rec.setVideoOption("vf", "scale=in_range=pc:out_range=pc,format=yuv420p");
            rec.start();
        }

        running.set(true);
        worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (running.get()) {
                    try {
                        BufferedImage img = WindowCapture.capture(hWnd);
                        if (img != null) {
                            // Immer in TYPE_3BYTE_BGR konvertieren -> stabile RGB/BGR-Interpretation
                            img = toBGR(img);

                            int iw = img.getWidth(), ih = img.getHeight();
                            if (iw != dim[0] || ih != dim[1]) {
                                dim[0] = makeEven(iw);
                                dim[1] = makeEven(ih);
                                BufferedImage scaled = new BufferedImage(dim[0], dim[1], BufferedImage.TYPE_3BYTE_BGR);
                                Graphics2D g = scaled.createGraphics();
                                try {
                                    g.drawImage(img, 0, 0, dim[0], dim[1], null);
                                } finally {
                                    g.dispose();
                                }
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
