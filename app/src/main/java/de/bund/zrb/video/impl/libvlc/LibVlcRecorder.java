package de.bund.zrb.video.impl.libvlc;

import de.bund.zrb.video.MediaRecorder;
import de.bund.zrb.video.RecordingProfile;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * LibVLC-based recorder implementation using vlcj.
 * Uses reflection to avoid hard dependency on vlcj at compile time.
 */
public final class LibVlcRecorder implements MediaRecorder {
    
    private static final boolean LOG_ENABLED = Boolean.getBoolean("wd4j.log.video");
    
    private Object mediaPlayerFactory;
    private Object mediaPlayer;
    private volatile boolean recording = false;
    
    /**
     * Creates a new LibVlcRecorder.
     * Requires vlcj to be on the classpath.
     * 
     * @throws Exception if vlcj is not available or initialization fails
     */
    public LibVlcRecorder() throws Exception {
        initializeVlcj();
    }
    
    private void initializeVlcj() throws Exception {
        try {
            // Load MediaPlayerFactory
            Class<?> factoryClass = Class.forName("uk.co.caprica.vlcj.player.component.MediaPlayerFactory");
            Constructor<?> factoryCtor = factoryClass.getConstructor();
            mediaPlayerFactory = factoryCtor.newInstance();
            
            log("MediaPlayerFactory initialized");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("vlcj not found on classpath. Add vlcj dependency or use FFmpeg backend.", e);
        }
    }
    
    @Override
    public void start(RecordingProfile profile) throws Exception {
        if (profile == null) {
            throw new IllegalArgumentException("RecordingProfile must not be null");
        }
        
        if (recording) {
            log("Already recording, stopping previous session");
            stop();
        }
        
        try {
            // Build sout (stream output) options from profile
            String sout = buildSoutOptions(profile);
            log("Starting recording with sout: " + sout);
            
            // Create MediaPlayer with options
            Class<?> factoryClass = mediaPlayerFactory.getClass();
            Method newMediaPlayerMethod = factoryClass.getMethod("newHeadlessMediaPlayer");
            mediaPlayer = newMediaPlayerMethod.invoke(mediaPlayerFactory);
            
            // Get media from source
            Class<?> playerClass = mediaPlayer.getClass();
            Method prepareMediaMethod = playerClass.getMethod("prepareMedia", String.class, String[].class);
            
            String[] options = new String[] { sout };
            prepareMediaMethod.invoke(mediaPlayer, profile.getSource(), options);
            
            // Start playback (which triggers recording)
            Method playMethod = playerClass.getMethod("play");
            playMethod.invoke(mediaPlayer);
            
            recording = true;
            log("Recording started successfully");
            
        } catch (Exception e) {
            recording = false;
            releaseResources();
            throw new Exception("Failed to start LibVLC recording: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void stop() {
        if (!recording) {
            return;
        }
        
        log("Stopping recording");
        recording = false;
        releaseResources();
    }
    
    @Override
    public boolean isRecording() {
        return recording;
    }
    
    private void releaseResources() {
        if (mediaPlayer != null) {
            try {
                Class<?> playerClass = mediaPlayer.getClass();
                Method stopMethod = playerClass.getMethod("stop");
                stopMethod.invoke(mediaPlayer);
                
                Method releaseMethod = playerClass.getMethod("release");
                releaseMethod.invoke(mediaPlayer);
                
                log("MediaPlayer released");
            } catch (Exception e) {
                log("Error releasing MediaPlayer: " + e.getMessage());
            }
            mediaPlayer = null;
        }
        
        if (mediaPlayerFactory != null) {
            try {
                Class<?> factoryClass = mediaPlayerFactory.getClass();
                Method releaseMethod = factoryClass.getMethod("release");
                releaseMethod.invoke(mediaPlayerFactory);
                
                log("MediaPlayerFactory released");
            } catch (Exception e) {
                log("Error releasing MediaPlayerFactory: " + e.getMessage());
            }
            mediaPlayerFactory = null;
        }
    }
    
    /**
     * Builds VLC sout (stream output) options from RecordingProfile.
     * Format: :sout=#transcode{vcodec=...,fps=...,width=...,height=...}:file{dst=...}
     */
    private String buildSoutOptions(RecordingProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append(":sout=#transcode{");
        
        // Video codec
        if (profile.getVideoCodec() != null && !profile.getVideoCodec().trim().isEmpty()) {
            sb.append("vcodec=").append(profile.getVideoCodec()).append(",");
        } else {
            // Default to h264 for better compatibility
            sb.append("vcodec=h264,");
        }
        
        // Audio codec (if specified)
        if (profile.getAudioCodec() != null && !profile.getAudioCodec().trim().isEmpty()) {
            sb.append("acodec=").append(profile.getAudioCodec()).append(",");
        }
        
        // FPS
        sb.append("fps=").append(profile.getFps()).append(",");
        
        // Dimensions
        sb.append("width=").append(profile.getWidth()).append(",");
        sb.append("height=").append(profile.getHeight());
        
        sb.append("}:file{dst=");
        sb.append(profile.getOutputFile().toString());
        sb.append("}");
        
        return sb.toString();
    }
    
    /**
     * Determines the appropriate source string for screen capture based on OS.
     * 
     * @return platform-specific screen capture source string
     */
    public static String getScreenCaptureSource() {
        String os = System.getProperty("os.name", "").toLowerCase();
        
        if (os.contains("win")) {
            // Windows: DirectShow screen capture
            return "screen://";
        } else if (os.contains("mac")) {
            // macOS: AVFoundation screen capture
            return "screen://";  // VLC handles this differently on macOS
        } else {
            // Linux: Video4Linux2 or X11
            return "screen://";
        }
    }
    
    private static void log(String message) {
        if (LOG_ENABLED) {
            System.out.println("[LibVlcRecorder] " + message);
        }
    }
}
