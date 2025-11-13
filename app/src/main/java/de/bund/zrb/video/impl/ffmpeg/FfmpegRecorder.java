package de.bund.zrb.video.impl.ffmpeg;

import de.bund.zrb.service.VideoRecordingService;
import de.bund.zrb.video.MediaRecorder;
import de.bund.zrb.video.RecordingProfile;

/**
 * FFmpeg/JavaCV-based recorder implementation.
 * Delegates to the existing VideoRecordingService and adapts RecordingProfile.
 */
public final class FfmpegRecorder implements MediaRecorder {
    
    private final VideoRecordingService delegate;
    
    public FfmpegRecorder() {
        this.delegate = VideoRecordingService.getInstance();
    }
    
    @Override
    public void start(RecordingProfile profile) throws Exception {
        if (profile == null) {
            throw new IllegalArgumentException("RecordingProfile must not be null");
        }
        
        // Map RecordingProfile to existing VideoRecordingService
        // Note: VideoRecordingService uses VideoConfig for FPS and other settings
        // The profile's outputFile is not directly used by VideoRecordingService 
        // (it generates its own timestamped filename), but we could extend it if needed.
        
        // For now, delegate uses its own logic for file naming and configuration.
        // The existing service doesn't accept per-call configuration, 
        // it relies on VideoConfig static settings.
        
        // Set FPS from profile if different from current config
        de.bund.zrb.config.VideoConfig.setFps(profile.getFps());
        
        // Set output directory from profile's output file parent
        if (profile.getOutputFile() != null && profile.getOutputFile().getParent() != null) {
            de.bund.zrb.config.VideoConfig.setReportsDir(
                profile.getOutputFile().getParent().toString()
            );
        }
        
        // Start recording using existing service
        delegate.start();
    }
    
    @Override
    public void stop() {
        delegate.stop();
    }
    
    @Override
    public boolean isRecording() {
        return delegate.isRecording();
    }
}
