package de.bund.zrb.video;

import de.bund.zrb.service.VideoRuntimeLoader;
import de.bund.zrb.video.impl.ffmpeg.FfmpegRecorder;
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
        // Settings-gesteuerte Backendwahl
        Boolean vlcEnabled = de.bund.zrb.service.SettingsService.getInstance().get("video.vlc.enabled", Boolean.class);
        if (vlcEnabled == null || vlcEnabled) {
            MediaRecorder libVlcRecorder = tryCreateLibVlcRecorder();
            if (libVlcRecorder != null) {
                System.out.println("Using LibVLC recorder");
                return libVlcRecorder;
            }
            System.out.println("LibVLC not available, falling back to FFmpeg/JavaCV");
        } else {
            System.out.println("VLC backend disabled via settings, using FFmpeg/JavaCV");
        }
        return new FfmpegRecorder();
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
            if (Boolean.TRUE.equals(autodetect)) {
                // Zuerst Autodetect (PATH/Registry)
                discovered = LibVlcLocator.useVlcjDiscovery();
            }
            if (!discovered && manualBase != null && !manualBase.trim().isEmpty()) {
                // Manueller Pfad erzwingen
                System.out.println("Trying manual VLC base path from settings: " + manualBase);
                discovered = LibVlcLocator.configureBasePath(manualBase.trim());
            }
            if (!discovered) {
                // Fallback: bekannte Standardpfade testen
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
