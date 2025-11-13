package de.bund.zrb.video;

/**
 * Strategy interface for video recording.
 * Allows different recording backends (LibVLC, FFmpeg/JavaCV) to be used interchangeably.
 */
public interface MediaRecorder {
    
    /**
     * Start recording with the given profile.
     * 
     * @param profile The recording configuration
     * @throws Exception if recording cannot be started
     */
    void start(RecordingProfile profile) throws Exception;
    
    /**
     * Stop the current recording.
     * No-op if not recording.
     */
    void stop();
    
    /**
     * Check if currently recording.
     * 
     * @return true if recording is in progress
     */
    boolean isRecording();
}
