package de.bund.zrb.video.impl.jcodec;

import com.sun.jna.platform.win32.WinDef;
import de.bund.zrb.BrowserImpl;
import de.bund.zrb.service.VideoRecordingService;
import de.bund.zrb.video.MediaRecorder;
import de.bund.zrb.video.RecordingProfile;
import de.bund.zrb.video.JcodecWindowRecorder;

import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;

/** MediaRecorder-Adapter für JCodec (reines Java, kein Audio). */
public final class JcodecRecorder implements MediaRecorder {

    private JcodecWindowRecorder wr;

    @Override
    public void start(RecordingProfile profile) throws Exception {
        if (wr != null) return;
        // HWND vom bereits initialisierten VideoRecordingService (dort wird es durch BrowserServiceImpl gesetzt)
        WinDef.HWND hwnd = de.bund.zrb.service.VideoRecordingService.getInstance().getTargetWindow();
        if (hwnd == null) throw new IllegalStateException("Kein Ziel-Fenster gesetzt (HWND=null)");

        Path out = profile.getOutputFile();
        if (out == null) throw new IllegalArgumentException("outputFile fehlt im Profile");
        try { Files.createDirectories(out.getParent()); } catch (Throwable ignore) {}

        int fps = profile.getFps() > 0 ? profile.getFps() : 15;
        wr = new JcodecWindowRecorder(hwnd, out, fps);
        wr.start();
    }

    @Override
    public void stop() {
        if (wr != null) { try { wr.stop(); } catch (Throwable ignore) {} wr = null; }
    }

    @Override
    public boolean isRecording() { return wr != null; }
}

