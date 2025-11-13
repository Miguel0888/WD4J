package de.bund.zrb.video;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MediaRuntimeBootstrap.
 */
public class MediaRuntimeBootstrapTest {
    
    @Test
    public void testCreateRecorder_returnsNonNull() {
        MediaRecorder recorder = MediaRuntimeBootstrap.createRecorder();
        assertNotNull(recorder);
    }
    
    @Test
    public void testGetPreferredBackend_returnsValidBackend() {
        String backend = MediaRuntimeBootstrap.getPreferredBackend();
        assertNotNull(backend);
        assertTrue(backend.equals("LibVLC") || backend.equals("FFmpeg"));
    }
    
    @Test
    public void testIsLibVlcAvailable_returnsBoolean() {
        // Just verify it doesn't throw
        boolean available = MediaRuntimeBootstrap.isLibVlcAvailable();
        // Can be true or false depending on environment
    }
}
