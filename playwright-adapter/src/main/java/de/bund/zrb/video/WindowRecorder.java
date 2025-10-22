package de.bund.zrb.video;

import de.bund.zrb.win.WindowCapture;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import com.sun.jna.platform.win32.WinDef;

import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class WindowRecorder implements Closeable {
    private final WinDef.HWND hWnd;
    private final Path outFile;
    private final int fps;
    private FFmpegFrameRecorder rec;
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Java2DFrameConverter conv = new Java2DFrameConverter();

    public WindowRecorder(WinDef.HWND hWnd, Path outFile, int fps) {
        this.hWnd = hWnd;
        this.outFile = outFile;
        this.fps = fps <= 0 ? 15 : fps;
    }

    public void start() throws Exception {
        if (running.get()) return;
        // erste Probe für Dimensionen
        BufferedImage probe = WindowCapture.capture(hWnd);
        if (probe == null) throw new IllegalStateException("WindowCapture liefert null");
        rec = new FFmpegFrameRecorder(outFile.toFile(), probe.getWidth(), probe.getHeight());
        rec.setFrameRate(fps);
        rec.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
        rec.setFormat("mp4");
        rec.setVideoOption("crf", "28");       // Qualität/Rate
        rec.setVideoOption("preset", "veryfast");
        rec.start();

        running.set(true);
        worker = new Thread(() -> {
            long frameIntervalMs = Math.max(1, Math.round(1000.0 / fps));
            while (running.get()) {
                try {
                    BufferedImage img = WindowCapture.capture(hWnd);
                    if (img != null) {
                        rec.record(conv.convert(img));
                    }
                    Thread.sleep(frameIntervalMs);
                } catch (Throwable t) {
                    // brich sauber ab, falls Fenster weg etc.
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
            try { worker.join(1500); } catch (InterruptedException ignored) {}
        }
        try { if (rec != null) rec.stop(); } catch (Exception ignored) {}
        try { if (rec != null) rec.release(); } catch (Exception ignored) {}
    }

    @Override public void close() { stop(); }
}
