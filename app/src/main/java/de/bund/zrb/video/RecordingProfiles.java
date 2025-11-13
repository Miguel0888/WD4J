package de.bund.zrb.video;

import com.sun.jna.platform.win32.WinDef;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Helper class to create RecordingProfile instances for common scenarios.
 */
public final class RecordingProfiles {
    
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    private RecordingProfiles() {}
    
    /**
     * Creates a window recording profile (Windows-specific).
     * 
     * @param hwnd The window handle to record
     * @param outputDir Output directory for the recording
     * @param fps Frames per second
     * @return RecordingProfile configured for window recording
     */
    public static RecordingProfile forWindowCapture(WinDef.HWND hwnd, Path outputDir, int fps) {
        if (hwnd == null) {
            throw new IllegalArgumentException("hwnd must not be null");
        }
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir must not be null");
        }
        
        // Generate timestamped filename
        String ts = LocalDateTime.now().format(TS);
        Path outfile = outputDir.resolve("video-" + ts + ".mkv");
        
        // For window capture, we use screen:// as source
        // The actual HWND filtering is handled by the backend implementation
        return RecordingProfile.builder()
                .source("screen://")
                .outputFile(outfile)
                .fps(fps)
                .build();
    }
    
    /**
     * Creates a screen recording profile with default settings.
     * 
     * @param outputFile Output file path
     * @param fps Frames per second
     * @return RecordingProfile configured for screen recording
     */
    public static RecordingProfile forScreenCapture(Path outputFile, int fps) {
        if (outputFile == null) {
            throw new IllegalArgumentException("outputFile must not be null");
        }
        
        return RecordingProfile.builder()
                .source("screen://")
                .outputFile(outputFile)
                .fps(fps)
                .build();
    }
    
    /**
     * Creates a recording profile from VideoConfig settings.
     * 
     * @return RecordingProfile based on current VideoConfig
     */
    public static RecordingProfile fromVideoConfig() {
        String baseDirStr = de.bund.zrb.config.VideoConfig.getReportsDir();
        if (baseDirStr == null || baseDirStr.trim().isEmpty()) {
            baseDirStr = "C:/Reports";
        }
        Path baseDir = Paths.get(baseDirStr);
        
        String ts = LocalDateTime.now().format(TS);
        Path outfile = baseDir.resolve("video-" + ts + ".mkv");
        
        int fps = Math.max(1, de.bund.zrb.config.VideoConfig.getFps());
        
        return RecordingProfile.builder()
                .source("screen://")
                .outputFile(outfile)
                .fps(fps)
                .videoCodec(de.bund.zrb.config.VideoConfig.getCodec())
                .build();
    }
}
