package de.bund.zrb.video;

import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RecordingProfile value object.
 */
public class RecordingProfileTest {
    
    @Test
    public void testBuilder_withValidData_createsProfile() {
        RecordingProfile profile = RecordingProfile.builder()
                .source("screen://")
                .outputFile(Paths.get("/tmp/test.mp4"))
                .width(1920)
                .height(1080)
                .fps(30)
                .videoCodec("h264")
                .audioCodec("aac")
                .build();
        
        assertEquals("screen://", profile.getSource());
        assertEquals(Paths.get("/tmp/test.mp4"), profile.getOutputFile());
        assertEquals(1920, profile.getWidth());
        assertEquals(1080, profile.getHeight());
        assertEquals(30, profile.getFps());
        assertEquals("h264", profile.getVideoCodec());
        assertEquals("aac", profile.getAudioCodec());
    }
    
    @Test
    public void testBuilder_withDefaults_usesDefaultValues() {
        RecordingProfile profile = RecordingProfile.builder()
                .source("screen://")
                .outputFile(Paths.get("/tmp/test.mp4"))
                .build();
        
        assertEquals(1920, profile.getWidth());
        assertEquals(1080, profile.getHeight());
        assertEquals(15, profile.getFps());
    }
    
    @Test
    public void testBuilder_withNullSource_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                RecordingProfile.builder()
                        .source(null)
                        .outputFile(Paths.get("/tmp/test.mp4"))
                        .build()
        );
    }
    
    @Test
    public void testBuilder_withEmptySource_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                RecordingProfile.builder()
                        .source("")
                        .outputFile(Paths.get("/tmp/test.mp4"))
                        .build()
        );
    }
    
    @Test
    public void testBuilder_withNullOutputFile_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                RecordingProfile.builder()
                        .source("screen://")
                        .outputFile(null)
                        .build()
        );
    }
    
    @Test
    public void testBuilder_withInvalidFps_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                RecordingProfile.builder()
                        .source("screen://")
                        .outputFile(Paths.get("/tmp/test.mp4"))
                        .fps(0)
                        .build()
        );
        
        assertThrows(IllegalArgumentException.class, () ->
                RecordingProfile.builder()
                        .source("screen://")
                        .outputFile(Paths.get("/tmp/test.mp4"))
                        .fps(-1)
                        .build()
        );
    }
    
    @Test
    public void testBuilder_withInvalidDimensions_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                RecordingProfile.builder()
                        .source("screen://")
                        .outputFile(Paths.get("/tmp/test.mp4"))
                        .width(0)
                        .build()
        );
        
        assertThrows(IllegalArgumentException.class, () ->
                RecordingProfile.builder()
                        .source("screen://")
                        .outputFile(Paths.get("/tmp/test.mp4"))
                        .height(-1)
                        .build()
        );
    }
    
    @Test
    public void testToString_includesAllFields() {
        RecordingProfile profile = RecordingProfile.builder()
                .source("screen://")
                .outputFile(Paths.get("/tmp/test.mp4"))
                .videoCodec("h264")
                .build();
        
        String str = profile.toString();
        assertTrue(str.contains("screen://"));
        assertTrue(str.contains("test.mp4"));
        assertTrue(str.contains("h264"));
    }
}
