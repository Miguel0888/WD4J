package de.bund.zrb.video.impl.libvlc;

import de.bund.zrb.video.MediaRecorder;
import de.bund.zrb.video.RecordingProfile;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

import java.awt.*;

/** Record via local VLC using vlcj 3.x. Keep options explicit and safe. */
public final class LibVlcRecorder implements MediaRecorder {

    private MediaPlayerFactory factory;
    private HeadlessMediaPlayer player;
    private volatile boolean recording;

    public LibVlcRecorder() {
        // Do not touch vlcj here; create lazily in start()
    }

    public void start(RecordingProfile profile) {
        if (recording) return;

        // Provide sane libvlc args to reduce noise and avoid stale cache warnings
        String[] libvlcArgs = new String[] {
                "--intf", "dummy",
                "--quiet",
                "--no-video-title-show",
                "--no-plugins-cache"
        };
        this.factory = new MediaPlayerFactory(libvlcArgs);
        this.player = factory.newHeadlessMediaPlayer();

        // Build VLC media options
        String mrl = toVlcMrl(profile);
        String[] options = buildOptions(profile);

        // Start capture
        boolean ok = player.playMedia(mrl, options);
        if (!ok) {
            safeRelease();
            throw new IllegalStateException("Failed to start LibVLC recording (playMedia returned false)");
        }
        recording = true;
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

    // Build a combination of source-module options and sout pipeline
    private String[] buildOptions(RecordingProfile p) {
        java.util.List<String> opts = new java.util.ArrayList<String>();

        // ----- Source module options (screen) -----
        String src = p.getSource() == null ? "screen://" : p.getSource();
        if (src.startsWith("screen://")) {
            int fps = Math.max(1, p.getFps() > 0 ? p.getFps() : 30);
            opts.add(":screen-fps=" + fps);

            // If no width/height provided, detect primary screen size
            int w = p.getWidth();
            int h = p.getHeight();
            if (w <= 0 || h <= 0) {
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                w = (int) screen.getWidth();
                h = (int) screen.getHeight();
            }
            opts.add(":screen-width=" + w);
            opts.add(":screen-height=" + h);
            // Optional: capture the whole screen; left/top default 0
            // opts.add(":screen-left=0");
            // opts.add(":screen-top=0");
        }

        // ----- Transcode/mux (sout) -----
        StringBuilder trans = new StringBuilder();
        trans.append(":sout=#transcode{");
        boolean wrote = false;

        String vCodec = getVideoCodec(p);
        if (vCodec == null || vCodec.trim().isEmpty()) vCodec = "h264"; // default
        trans.append("vcodec=").append(vCodec);
        wrote = true;

        // Scale using transcode (optional). Keep original if not set.
        if (p.getWidth() > 0 && p.getHeight() > 0) {
            trans.append(",width=").append(p.getWidth()).append(",height=").append(p.getHeight());
        }
        if (p.getFps() > 0) {
            trans.append(",fps=").append(p.getFps());
        }

        // Audio: screen:// has no audio – avoid acodec unless explicitly requested
        String aCodec = getAudioCodec(p);
        if (aCodec != null && aCodec.trim().length() > 0) {
            trans.append(",acodec=").append(aCodec);
        } else {
            trans.append(",acodec=none");
        }

        trans.append("}:std{access=file,mux=mp4,dst=").append(p.getOutputFile()).append("}");
        opts.add(trans.toString());

        // Keep pipeline alive
        opts.add(":sout-keep");

        return opts.toArray(new String[opts.size()]);
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
