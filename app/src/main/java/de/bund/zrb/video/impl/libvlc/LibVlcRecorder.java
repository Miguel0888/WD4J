package de.bund.zrb.video.impl.libvlc;

import de.bund.zrb.service.SettingsService;
import de.bund.zrb.video.MediaRecorder;
import de.bund.zrb.video.RecordingProfile;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/** Record via local VLC using vlcj 3.x. Keep options explicit and safe. */
public final class LibVlcRecorder implements MediaRecorder {

    private MediaPlayerFactory factory;
    private HeadlessMediaPlayer player;
    private volatile boolean recording;

    public LibVlcRecorder() {
        // Do not touch vlcj here; create lazily in start()
    }

    // in LibVlcRecorder
    private java.nio.file.Path currentOutPath; // tatsächlicher Zielpfad dieser Aufnahme

    public void start(RecordingProfile profile) {
        if (recording) return;

        String[] libvlcArgs = new String[] {
                "--intf", "dummy",
                "--no-video-title-show",
                "--no-plugins-cache",
                "--file-logging",
                "--logfile=" + resolveLogPath(),
                "--verbose=2"
        };
        this.factory = new MediaPlayerFactory(libvlcArgs);
        this.player = factory.newHeadlessMediaPlayer();

        player.addMediaPlayerEventListener(new uk.co.caprica.vlcj.player.MediaPlayerEventAdapter() {
            @Override public void error(uk.co.caprica.vlcj.player.MediaPlayer mp) {
                System.out.println("[LibVlcRecorder] ERROR event from media player");
            }
            @Override public void playing(uk.co.caprica.vlcj.player.MediaPlayer mp) {
                System.out.println("[LibVlcRecorder] Playing started");
            }
            @Override public void stopped(uk.co.caprica.vlcj.player.MediaPlayer mp) {
                System.out.println("[LibVlcRecorder] Stopped");
            }
            @Override public void finished(uk.co.caprica.vlcj.player.MediaPlayer mp) {
                System.out.println("[LibVlcRecorder] Finished");
            }
        });

        // Build options and remember the exact output path used for this run (TS for debug)
        String mrl = toVlcMrl(profile);
        String[] options = buildOptionsAndRememberOutPath(profile); // sets currentOutPath

        System.out.println("[LibVlcRecorder] MRL: " + mrl);
        for (int i = 0; i < options.length; i++) System.out.println("[LibVlcRecorder] opt[" + i + "]: " + options[i]);

        // Ensure parent dir exists and remove old file; DO NOT pre-create the file
        try {
            if (currentOutPath != null && currentOutPath.getParent() != null) {
                java.nio.file.Files.createDirectories(currentOutPath.getParent());
            }
            if (currentOutPath != null) {
                java.nio.file.Files.deleteIfExists(currentOutPath);
            }
        } catch (Exception ignore) { /* ignore */ }

        boolean ok = player.playMedia(mrl, options);
        if (!ok) {
            safeRelease();
            throw new IllegalStateException("Failed to start LibVLC recording (playMedia returned false)");
        }
        recording = true;

        // Probe file growth for diagnostics (TS path)
        final java.nio.file.Path probe = currentOutPath;
        new Thread(new Runnable() {
            public void run() {
                try {
                    for (int i = 0; i < 6; i++) {
                        Thread.sleep(500);
                        long size = -1L;
                        try { size = (probe != null && java.nio.file.Files.exists(probe)) ? java.nio.file.Files.size(probe) : -1L; } catch (Exception ignore) {}
                        System.out.println("[LibVlcRecorder] out size after " + (i+1)*500 + "ms: " + size + " (" + (probe != null ? probe.toString() : "null") + ")");
                    }
                } catch (InterruptedException ignore) {}
            }
        }).start();
    }

    private String[] buildOptionsAndRememberOutPath(RecordingProfile p) {
        java.util.List<String> opts = new java.util.ArrayList<String>();

        // screen module
        String src = p.getSource() == null ? "screen://" : p.getSource();
        if (src.startsWith("screen://")) {
            int fps = Math.max(1, p.getFps() > 0 ? p.getFps() : 30);
            opts.add(":screen-fps=" + fps);
            int w = p.getWidth(), h = p.getHeight();
            if (w <= 0 || h <= 0) {
                java.awt.Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                w = (int) d.getWidth(); h = (int) d.getHeight();
            }
            opts.add(":screen-width=" + w);
            opts.add(":screen-height=" + h);
        }

        // Robust first run: TS, no explicit venc
        String vCodec = getVideoCodec(p);
        if (vCodec == null || vCodec.trim().isEmpty()) vCodec = "h264";

        // Compute the actual output path for THIS run (TS extension)
        java.nio.file.Path base = resolveOutputPath(p);
        java.nio.file.Path tsPath = rewriteExtension(base, ".ts");

        this.currentOutPath = tsPath; // remember
        String dst = normalizeAndQuotePath(tsPath.toAbsolutePath().toString());

//        this.currentOutPath = java.nio.file.Paths.get("C:/Temp/wd4j-test.ts");
//        String dst = normalizeAndQuotePath(this.currentOutPath.toString());

        StringBuilder sout = new StringBuilder();
        sout.append(":sout=#transcode{");
        sout.append("vcodec=").append(vCodec);
        if (p.getFps() > 0) sout.append(",fps=").append(p.getFps());
        if (p.getWidth() > 0 && p.getHeight() > 0) {
            sout.append(",width=").append(p.getWidth()).append(",height=").append(p.getHeight());
        }
        sout.append(",acodec=none");
        sout.append("}:std{access=file,mux=ts,dst=").append(dst).append("}");
        opts.add(sout.toString());
        opts.add(":sout-keep");

        return opts.toArray(new String[opts.size()]);
    }

    private String resolveLogPath() {
        String home = System.getProperty("user.home");
        return (home + "/.wd4j/vlc.log").replace('\\','/');
    }

    public void stop() {
        if (!recording) return;
        try {
            player.stop();
        } finally {
            safeRelease();
            recording = false;
        }
    }

    public boolean isRecording() {
        return recording;
    }

    private void safeRelease() {
        try { if (player != null) player.release(); } catch (Throwable ignore) {}
        try { if (factory != null) factory.release(); } catch (Throwable ignore) {}
        player = null;
        factory = null;
    }

    // Use screen capture by default; allow pass-through of known MRLs
    private String toVlcMrl(RecordingProfile p) {
        String src = p.getSource();
        if (src == null) return "screen://";
        if (src.startsWith("screen://") || src.startsWith("dshow://")
                || src.startsWith("v4l2://") || src.startsWith("avfoundation://")) {
            return src;
        }
        return "screen://";
    }

    // Build a combination of source-module options and sout pipeline (first run: mux=ts)
    private String[] buildOptions(RecordingProfile p) {
        java.util.List<String> opts = new java.util.ArrayList<String>();

        // --- screen module ---
        String src = p.getSource() == null ? "screen://" : p.getSource();
        if (src.startsWith("screen://")) {
            int fps = Math.max(1, p.getFps() > 0 ? p.getFps() : 30);
            opts.add(":screen-fps=" + fps);

            int w = p.getWidth();
            int h = p.getHeight();
            if (w <= 0 || h <= 0) {
                java.awt.Dimension d = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
                w = (int) d.getWidth();
                h = (int) d.getHeight();
            }
            opts.add(":screen-width=" + w);
            opts.add(":screen-height=" + h);
            // opts.add(":screen-caching=100"); // optional
        }

        // --- transcode + std (robust: TS, no explicit venc) ---
        String vCodec = getVideoCodec(p);
        if (vCodec == null || vCodec.trim().isEmpty()) vCodec = "h264";

        String dst = normalizeAndQuotePath(rewriteExtension(resolveOutputPathString(p), ".ts"));

        StringBuilder sout = new StringBuilder();
        sout.append(":sout=#transcode{");
        sout.append("vcodec=").append(vCodec);
        if (p.getFps() > 0) sout.append(",fps=").append(p.getFps());
        if (p.getWidth() > 0 && p.getHeight() > 0) {
            sout.append(",width=").append(p.getWidth()).append(",height=").append(p.getHeight());
        }
        sout.append(",acodec=none");
        sout.append("}:std{access=file,mux=ts,dst=").append(dst).append("}");

        opts.add(sout.toString());
        opts.add(":sout-keep");
        return opts.toArray(new String[opts.size()]);
    }

    // --- Helpers: resolve output path regardless of profile's getter type ---

    /** Resolve output path as Path whether profile stores String or Path. */
    // Resolve output Path regardless of profile having String or Path
    private java.nio.file.Path resolveOutputPath(RecordingProfile p) {
        try {
            java.lang.reflect.Method m = RecordingProfile.class.getMethod("getOutputFile");
            Object r = m.invoke(p);
            if (r instanceof java.nio.file.Path) return (java.nio.file.Path) r;
            if (r instanceof String) return java.nio.file.Paths.get((String) r);
        } catch (Throwable ignore) {}
        try {
            java.lang.reflect.Method m2 = RecordingProfile.class.getMethod("getOutputPath");
            Object r2 = m2.invoke(p);
            if (r2 instanceof java.nio.file.Path) return (java.nio.file.Path) r2;
            if (r2 instanceof String) return java.nio.file.Paths.get((String) r2);
        } catch (Throwable ignore) {}
        return java.nio.file.Paths.get("capture.mp4");
    }

    private String normalizeAndQuotePath(String path) {
        if (path == null || path.trim().isEmpty()) return "\"capture.ts\"";
        String normalized = path.replace('\\', '/');
        return "\"" + normalized + "\"";
    }

    private java.nio.file.Path rewriteExtension(java.nio.file.Path original, String newExt) {
        String s = original.toAbsolutePath().toString();
        int i = s.lastIndexOf('.');
        String base = (i > 0 ? s.substring(0, i) : s);
        return java.nio.file.Paths.get(base + newExt);
    }

    /** Resolve output path as String for VLC command line (forward slashes). */
    private String resolveOutputPathString(RecordingProfile p) {
        Path path = resolveOutputPath(p);
        return path.toAbsolutePath().toString();
    }

    private String rewriteExtension(String original, String newExt) {
        if (original == null) return "capture" + newExt;
        int i = original.lastIndexOf('.');
        String base = (i > 0 ? original.substring(0, i) : original);
        return base + newExt;
    }

    // Support either getVideoCodec()/getAudioCodec() or getvCodec()/getaCodec()
    private String getVideoCodec(RecordingProfile p) {
        try { return (String) RecordingProfile.class.getMethod("getVideoCodec").invoke(p); }
        catch (Throwable ignore) { /* fall back */ }
        try { return (String) RecordingProfile.class.getMethod("getvCodec").invoke(p); }
        catch (Throwable ignore) { return null; }
    }

    private String getAudioCodec(RecordingProfile p) {
        try { return (String) RecordingProfile.class.getMethod("getAudioCodec").invoke(p); }
        catch (Throwable ignore) { /* fall back */ }
        try { return (String) RecordingProfile.class.getMethod("getaCodec").invoke(p); }
        catch (Throwable ignore) { return null; }
    }
}
