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
        // Try LibVLC first
        MediaRecorder libVlcRecorder = tryCreateLibVlcRecorder();
        if (libVlcRecorder != null) {
            System.out.println("Using LibVLC recorder");
            return libVlcRecorder;
        }
        
        // Fallback to FFmpeg/JavaCV
        System.out.println("LibVLC not available, falling back to FFmpeg/JavaCV");
        return new FfmpegRecorder();
    }
    
    /**
     * Creates a MediaRecorder instance with interactive fallback.
     * If FFmpeg/JavaCV is not available, prompts user to download libraries.
     * 
     * @return MediaRecorder instance or null if user cancels
     */
    public static MediaRecorder createRecorderInteractive() {
        // Try LibVLC first
        MediaRecorder libVlcRecorder = tryCreateLibVlcRecorder();
        if (libVlcRecorder != null) {
            System.out.println("Using LibVLC recorder");
            return libVlcRecorder;
        }
        
        // Fallback to FFmpeg/JavaCV with interactive loading
        System.out.println("LibVLC not available, trying FFmpeg/JavaCV");
        
        // Ensure video libraries are available (may prompt user)
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

            // Try discovery (PATH/Registry). If that fails, try manual path configure
            boolean discovered = de.bund.zrb.video.impl.libvlc.LibVlcLocator.useVlcjDiscovery();
            if (!discovered) {
                System.out.println("NativeDiscovery failed; try manual locate+configure");
                discovered = de.bund.zrb.video.impl.libvlc.LibVlcLocator.locateAndConfigure();
            }
            if (!discovered) {
                System.out.println("VLC installation not found");
                return null;
            }

            // Create v3 recorder
            return new de.bund.zrb.video.impl.libvlc.LibVlcRecorder();

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
