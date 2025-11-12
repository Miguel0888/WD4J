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
            log("Using LibVLC recorder");
            return libVlcRecorder;
        }
        
        // Fallback to FFmpeg/JavaCV
        log("LibVLC not available, falling back to FFmpeg/JavaCV");
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
            log("Using LibVLC recorder");
            return libVlcRecorder;
        }
        
        // Fallback to FFmpeg/JavaCV with interactive loading
        log("LibVLC not available, trying FFmpeg/JavaCV");
        
        // Ensure video libraries are available (may prompt user)
        boolean available = VideoRuntimeLoader.ensureVideoLibsAvailableInteractively();
        if (!available) {
            log("User cancelled video library download");
            return null;
        }
        
        return new FfmpegRecorder();
    }
    
    /**
     * Attempts to create a LibVLC recorder.
     * 
     * @return LibVlcRecorder instance or null if not available
     */
    private static MediaRecorder tryCreateLibVlcRecorder() {
        try {
            // Check if vlcj is on classpath
            if (!LibVlcLocator.isVlcjAvailable()) {
                log("vlcj not on classpath");
                return null;
            }
            
            // Try to locate VLC installation
            boolean located = LibVlcLocator.locateAndConfigure();
            if (!located) {
                log("VLC installation not found");
                // Try vlcj's own discovery as fallback
                if (!LibVlcLocator.useVlcjDiscovery()) {
                    log("vlcj NativeDiscovery also failed");
                    return null;
                }
            }
            
            // Try to create LibVlcRecorder
            return new LibVlcRecorder();
            
        } catch (Throwable t) {
            log("Failed to create LibVlcRecorder: " + t.getMessage());
            if (LOG_ENABLED) {
                t.printStackTrace();
            }
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
    
    private static void log(String message) {
        if (LOG_ENABLED) {
            System.out.println("[MediaRuntimeBootstrap] " + message);
        }
    }
}
