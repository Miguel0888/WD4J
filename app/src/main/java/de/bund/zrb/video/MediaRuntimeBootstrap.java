package de.bund.zrb.video;

import de.bund.zrb.service.VideoRuntimeLoader;
import de.bund.zrb.video.impl.ffmpeg.FfmpegRecorder;
import de.bund.zrb.video.impl.jcodec.JcodecRecorder;
import de.bund.zrb.video.impl.libvlc.LibVlcLocator;
import de.bund.zrb.video.impl.libvlc.LibVlcRecorder;

/**
 * Bootstrap class for selecting the appropriate MediaRecorder implementation.
 * Tries LibVLC first (if available locally), falls back to FFmpeg/JavaCV.
 */
public final class MediaRuntimeBootstrap {
    
    private static final boolean LOG_ENABLED = Boolean.getBoolean("wd4j.log.video");
    
    private MediaRuntimeBootstrap() {}
    
    /**
     * Creates a MediaRecorder instance, preferring LibVLC if available.
     * 
     * @return MediaRecorder instance (LibVLC or FFmpeg)
     * @throws IllegalStateException if no recorder backend is available
     */
    public static MediaRecorder createRecorder() {
        String backend = de.bund.zrb.service.SettingsService.getInstance().get("video.backend", String.class);
        if (backend != null) backend = backend.trim().toLowerCase(java.util.Locale.ROOT);
        if (backend == null || backend.isEmpty()) backend = "jcodec";

        switch (backend) {
            case "vlc": {
                MediaRecorder r = tryCreateLibVlcRecorder();
                if (r == null) throw new IllegalStateException("VLC ausgewählt, aber LibVLC nicht verfügbar");
                return r;
            }
            case "ffmpeg": {
                if (!de.bund.zrb.service.VideoRecordingService.quickCheckAvailable()) {
                    throw new IllegalStateException("FFmpeg/JavaCV ausgewählt, aber nicht verfügbar");
                }
                return new FfmpegRecorder();
            }
            case "jcodec": {
                return new JcodecRecorder();
            }
            default:
                throw new IllegalArgumentException("Unbekanntes Backend: " + backend);
        }
    }
    
    /**
     * Creates a MediaRecorder instance with interactive fallback.
     * If FFmpeg/JavaCV is not available, prompts user to download libraries.
     * 
     * @return MediaRecorder instance or null if user cancels
     */
    public static MediaRecorder createRecorderInteractive() {
        Boolean vlcEnabled = de.bund.zrb.service.SettingsService.getInstance().get("video.vlc.enabled", Boolean.class);
        if (vlcEnabled == null || vlcEnabled) {
            MediaRecorder libVlcRecorder = tryCreateLibVlcRecorder();
            if (libVlcRecorder != null) {
                System.out.println("Using LibVLC recorder");
                return libVlcRecorder;
            }
        }
        System.out.println("LibVLC not available, trying FFmpeg/JavaCV");
        boolean available = VideoRuntimeLoader.ensureVideoLibsAvailableInteractively();
        if (!available) {
            System.out.println("User cancelled video library download");
            return null;
        }
        return new FfmpegRecorder();
    }
    
    /**
     * Attempts to create a LibVLC recorder.
     * 
     * @return LibVlcRecorder instance or null if not available
     */
    public static MediaRecorder tryCreateLibVlcRecorder() {
        try {
            // If vlcj 3.x is missing, fail gracefully
            try {
                Class.forName("uk.co.caprica.vlcj.player.MediaPlayerFactory");
            } catch (ClassNotFoundException e) {
                System.out.println("vlcj 3.x not on classpath");
                return null;
            }

            // Settings: Autodetect & manueller Pfad
            de.bund.zrb.service.SettingsService s = de.bund.zrb.service.SettingsService.getInstance();
            Boolean autodetect = s.get("video.vlc.autodetect", Boolean.class);
            String manualBase  = s.get("video.vlc.basePath", String.class);

            boolean discovered = false;

            // 1) Wenn manueller Pfad vorhanden, zuerst diesen konfigurieren
            if (manualBase != null && !manualBase.trim().isEmpty()) {
                System.out.println("Trying manual VLC base path from settings: " + manualBase);
                discovered = LibVlcLocator.configureBasePath(manualBase.trim());
            }

            // 2) Falls noch nicht erfolgreich und Autodetect an, Discovery nutzen
            if (!discovered && Boolean.TRUE.equals(autodetect)) {
                System.out.println("Manual path not sufficient; trying NativeDiscovery");
                discovered = LibVlcLocator.useVlcjDiscovery();
            }

            // 3) Falls immer noch nicht, bekannte Standardpfade testen
            if (!discovered) {
                System.out.println("Discovery/manual base failed; try known locations");
                discovered = LibVlcLocator.locateAndConfigure();
            }

            if (!discovered) {
                System.out.println("VLC installation not found");
                return null;
            }

            // Create v3 recorder
            return new LibVlcRecorder();

        } catch (Throwable t) {
            System.out.println("Failed to create LibVlcRecorder: " + t.getMessage());
            if (LOG_ENABLED) t.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if LibVLC recorder is available on this system.
     * 
     * @return true if LibVLC can be used
     */
    public static boolean isLibVlcAvailable() {
        return tryCreateLibVlcRecorder() != null;
    }
    
    /**
     * Gets the name of the currently preferred recorder backend.
     * 
     * @return "LibVLC" or "FFmpeg"
     */
    public static String getPreferredBackend() {
        return isLibVlcAvailable() ? "LibVLC" : "FFmpeg";
    }

    public static MediaRecorder createFfmpegRecorder() {
        // Validate availability of the FFmpeg/JavaCV stack before constructing the adapter
        // Comments in English and imperative style
        if (!de.bund.zrb.service.VideoRecordingService.quickCheckAvailable()) {
            throw new IllegalStateException("FFmpeg/JavaCV stack not available. Prepare libraries before calling createFfmpegRecorder().");
        }
        System.out.println("Using FFmpeg/JavaCV recorder");
        return new FfmpegRecorder();
    }
}
