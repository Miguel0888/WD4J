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

        // Provide sane libvlc args to reduce noise and optional logging
        String[] libvlcArgs = buildLibVlcArgs();
        this.factory = new MediaPlayerFactory(libvlcArgs);
        this.player = factory.newHeadlessMediaPlayer();

        // Build VLC media options
        String mrl = toVlcMrl(profile);
        String[] options = buildOptions(profile);

        boolean ok = player.playMedia(mrl, options);
        if (!ok) {
            safeRelease();
            throw new IllegalStateException("Failed to start LibVLC recording (playMedia returned false)");
        }
        recording = true;
    }

    private String[] buildLibVlcArgs() {
        java.util.ArrayList<String> args = new java.util.ArrayList<>();
        args.add("--intf"); args.add("dummy");
        args.add("--no-video-title-show");
        args.add("--no-plugins-cache");
        // Optional: quiet und verbose aus Settings ableiten
        de.bund.zrb.service.SettingsService s = de.bund.zrb.service.SettingsService.getInstance();
        Integer v = s.get("video.vlc.verbose", Integer.class);
        int verbose = v == null ? 1 : Math.max(0, Math.min(2, v));
        if (verbose == 0) args.add("--quiet"); else args.add("--verbose=" + verbose);
        // Optionales Datei-Logging per Settings
        try {
            Boolean logEnabled = s.get("video.vlc.log.enabled", Boolean.class);
            String  logPath    = s.get("video.vlc.log.path", String.class);
            if (Boolean.TRUE.equals(logEnabled)) {
                args.add("--file-logging");
                if (logPath != null && !logPath.trim().isEmpty()) {
                    args.add("--logfile=" + normalizePath(logPath));
                }
            }
        } catch (Throwable ignore) {}
        return args.toArray(new String[0]);
    }

    private static String normalizePath(String p) { return p.replace('\\', '/'); }

    public void stop() {
        if (!recording) return;
        try { player.stop(); } finally { safeRelease(); recording = false; }
    }

    public boolean isRecording() { return recording; }

    private void safeRelease() {
        try { if (player != null) player.release(); } catch (Throwable ignore) {}
        try { if (factory != null) factory.release(); } catch (Throwable ignore) {}
        player = null; factory = null;
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
        java.util.List<String> opts = new java.util.ArrayList<>();
        de.bund.zrb.service.SettingsService s = de.bund.zrb.service.SettingsService.getInstance();

        // ----- Source module options (screen) -----
        String src = p.getSource() == null ? "screen://" : p.getSource();
        if (src.startsWith("screen://")) {
            int fps = Math.max(1, p.getFps() > 0 ? p.getFps() : 30);
            opts.add(":screen-fps=" + fps);

            // Fullscreen oder Region
            boolean fullscreen = Boolean.TRUE.equals(s.get("video.vlc.screen.fullscreen", Boolean.class));
            int left  = orInt(s.get("video.vlc.screen.left", Integer.class), 0);
            int top   = orInt(s.get("video.vlc.screen.top", Integer.class), 0);
            int width = p.getWidth();
            int height= p.getHeight();
            // Settings-Override nur wenn nicht fullscreen
            if (!fullscreen) {
                int w = orInt(s.get("video.vlc.screen.width", Integer.class), 0);
                int h = orInt(s.get("video.vlc.screen.height", Integer.class), 0);
                if (w > 0) width = w;
                if (h > 0) height = h;
                opts.add(":screen-left=" + Math.max(0, left));
                opts.add(":screen-top=" + Math.max(0, top));
            } else {
                // ggf. Monitordimension
                if (width <= 0 || height <= 0) {
                    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                    width = (int) screen.getWidth();
                    height= (int) screen.getHeight();
                }
            }
            if (width <= 0 || height <= 0) {
                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                width = (int) screen.getWidth();
                height= (int) screen.getHeight();
            }
            opts.add(":screen-width=" + width);
            opts.add(":screen-height=" + height);

            // optionale video filter und deinterlace
            String vFilter = s.get("video.vlc.videoFilter", String.class);
            if (vFilter != null && !vFilter.trim().isEmpty()) {
                opts.add(":video-filter=" + vFilter.trim());
            }
            Boolean deint = s.get("video.vlc.deinterlace.enabled", Boolean.class);
            if (Boolean.TRUE.equals(deint)) {
                opts.add(":deinterlace=1");
                String mode = s.get("video.vlc.deinterlace.mode", String.class);
                if (mode != null && !mode.trim().isEmpty()) {
                    opts.add(":deinterlace-mode=" + mode.trim());
                }
            }
        }

        // ----- Transcode/mux (sout) -----
        String vCodec = firstNonEmpty(p.getVideoCodec(), s.get("video.vlc.vcodec", String.class), "h264");
        String mux = orString(s.get("video.vlc.mux", String.class), "mp4");
        String quality = orString(s.get("video.vlc.quality", String.class), "crf");
        int crf = orInt(s.get("video.vlc.crf", Integer.class), 23);
        int bitrate = orInt(s.get("video.vlc.bitrateKbps", Integer.class), 4000);
        String preset = s.get("video.vlc.venc.preset", String.class);
        String tune   = s.get("video.vlc.venc.tune", String.class);
        boolean audio = Boolean.TRUE.equals(s.get("video.vlc.audio.enabled", Boolean.class));
        String soutExtras = s.get("video.vlc.soutExtras", String.class);

        StringBuilder trans = new StringBuilder();
        trans.append(":sout=#transcode{");
        trans.append("vcodec=").append(vCodec);
        if (p.getFps() > 0) trans.append(",fps=").append(p.getFps());
        if (p.getWidth() > 0 && p.getHeight() > 0) {
            trans.append(",width=").append(p.getWidth()).append(",height=").append(p.getHeight());
        }
        if ("crf".equalsIgnoreCase(quality)) {
            trans.append(",venc=x264{crf=").append(crf);
            if (notEmpty(preset)) trans.append(",preset=").append(preset);
            if (notEmpty(tune))   trans.append(",tune=").append(tune);
            trans.append("}");
        } else if ("bitrate".equalsIgnoreCase(quality)) {
            if (bitrate > 0) trans.append(",vb=").append(bitrate).append("k");
        }
        if (audio) {
            trans.append(",acodec=mp3,ab=128,channels=2,samplerate=44100");
        } else {
            trans.append(",acodec=none");
        }
        trans.append("}");
        if (soutExtras != null && !soutExtras.trim().isEmpty()) {
            if (!soutExtras.startsWith(",")) trans.append(",");
            trans.append(soutExtras.trim());
        }
        String dst = normalizePath(p.getOutputFile().toString());
        trans.append(":std{access=file,mux=").append(mux).append(",dst=").append(dst).append("}");
        opts.add(trans.toString());
        opts.add(":sout-keep");

        return opts.toArray(new String[0]);
    }

    private static boolean notEmpty(String s) { return s != null && !s.trim().isEmpty(); }
    private static String orString(String v, String d) { return (v == null || v.trim().isEmpty()) ? d : v.trim(); }
    private static int orInt(Integer v, int d) { return v == null ? d : v; }
    private static String firstNonEmpty(String a, String b, String d) {
        if (a != null && !a.trim().isEmpty()) return a.trim();
        if (b != null && !b.trim().isEmpty()) return b.trim();
        return d;
    }

    // Support either getVideoCodec()/getAudioCodec() or getvCodec()/getaCodec() – hier nur Video nötig
    private String getVideoCodec(RecordingProfile p) {
        try { return (String) RecordingProfile.class.getMethod("getVideoCodec").invoke(p); }
        catch (Throwable ignore) { return null; }
    }

    // Configure VLC 64-bit path explicitly and verify bitness early
    private static void configureVlc64(String explicitVlcHome) {
        // Log basic environment to diagnose bitness issues
        String arch = System.getProperty("os.arch");
        System.out.println("[VLC] JVM os.arch=" + arch + "  expected: amd64/x86_64");
        if (!arch.contains("64")) {
            throw new IllegalStateException("Running on 32-bit JVM; install/use a 64-bit JVM to load 64-bit VLC.");
        }

        // Prefer explicit path; fall back to standard 64-bit location
        String vlcHome = explicitVlcHome != null ? explicitVlcHome : "C:\\\\Program Files\\\\VideoLAN\\\\VLC";
        java.io.File dll = new java.io.File(vlcHome, "libvlc.dll");
        if (!dll.exists()) {
            throw new IllegalStateException("libvlc.dll not found at: " + dll.getAbsolutePath());
        }
        if (vlcHome.contains("Program Files (x86)")) {
            throw new IllegalStateException("VLC path points to 32-bit installation. Use 64-bit VLC under 'Program Files'.");
        }

        // Set search paths BEFORE touching vlcj/JNA
        String sep = System.getProperty("path.separator");
        String existing = System.getProperty("jna.library.path");
        String combined = (existing == null || existing.isEmpty()) ? vlcHome : existing + sep + vlcHome;
        System.setProperty("jna.library.path", combined);

        java.io.File plugins = new java.io.File(vlcHome, "plugins");
        if (plugins.exists()) {
            System.setProperty("VLC_PLUGIN_PATH", plugins.getAbsolutePath());
        }

        // Optional: also help PATH resolution (some setups rely on it)
        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || !pathEnv.toLowerCase().contains(vlcHome.toLowerCase())) {
            String newPath = vlcHome + ";" + pathEnv;
            try {
                // JNA cannot set process env PATH portably; just log the hint
                System.out.println("[VLC] Consider adding to PATH: " + vlcHome);
            } catch (Throwable ignore) { /* ignore */ }
        }

        System.out.println("[VLC] jna.library.path=" + System.getProperty("jna.library.path"));
        System.out.println("[VLC] VLC_PLUGIN_PATH=" + System.getProperty("VLC_PLUGIN_PATH"));
    }

}
