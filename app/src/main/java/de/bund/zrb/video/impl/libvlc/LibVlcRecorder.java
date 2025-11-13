package de.bund.zrb.video.impl.libvlc;

import de.bund.zrb.video.MediaRecorder;
import de.bund.zrb.video.RecordingProfile;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

/** Implement recording via LibVLC using vlcj 3.x (Java 8). */
public final class LibVlcRecorder implements MediaRecorder {

    private MediaPlayerFactory factory;
    private HeadlessMediaPlayer player;
    private volatile boolean recording;

    public LibVlcRecorder() {
        // Do not touch vlcj here; create lazily in start()
    }

    public void start(RecordingProfile profile) {
        if (recording) return;

        // Create factory/player lazily after discovery is done
        this.factory = new MediaPlayerFactory();
        this.player = factory.newHeadlessMediaPlayer();

        // Build VLC sout options
        String sout = buildSout(profile);
        String[] options = new String[] { sout, ":sout-keep" };

        // Build MRL from generic source
        String mrl = toVlcMrl(profile);

        // Start capture
        player.playMedia(mrl, options);
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

    private String toVlcMrl(RecordingProfile profile) {
        String src = profile.getSource();
        if (src == null) return "screen://";
        if (src.startsWith("dshow://") || src.startsWith("v4l2://") ||
                src.startsWith("avfoundation://") || src.startsWith("screen://")) {
            return src;
        }
        return "screen://";
    }

    private String buildSout(RecordingProfile profile) {
        StringBuilder t = new StringBuilder();
        t.append(":sout=#transcode{");
        boolean wrote = false;

        String v = getVideoCodec(profile);
        if (v != null) {
            t.append("vcodec=").append(v);
            wrote = true;
            if (profile.getFps() > 0) t.append(",fps=").append(profile.getFps());
            if (profile.getWidth() > 0 && profile.getHeight() > 0) {
                t.append(",width=").append(profile.getWidth()).append(",height=").append(profile.getHeight());
            }
        }
        String a = getAudioCodec(profile);
        if (a != null) {
            if (wrote) t.append(",");
            t.append("acodec=").append(a);
        }
        t.append("}:std{access=file,mux=mp4,dst=").append(profile.getOutputFile()).append("}");
        return t.toString();
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
