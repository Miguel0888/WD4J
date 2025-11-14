package de.bund.zrb.video.overlay;

import de.bund.zrb.video.OverlayBridge;

/** Default-Sink: delegiert an die vorhandene OverlayBridge. Stilwerte werden aktuell nicht ausgewertet. */
public final class DefaultVideoOverlaySink implements VideoOverlaySink {
    @Override
    public void setCaption(String text, VideoOverlayStyle style) {
        OverlayBridge.setCaption(text);
    }

    @Override
    public void clearCaption() {
        OverlayBridge.clearCaption();
    }

    @Override
    public void setSubtitle(String text, VideoOverlayStyle style) {
        OverlayBridge.setSubtitle(text);
    }

    @Override
    public void clearSubtitle() {
        OverlayBridge.clearSubtitle();
    }

    @Override
    public void showTransient(String text, VideoOverlayStyle style, long millis) {
        if (text == null || text.trim().isEmpty()) return;
        // Verwende Subtitle als transienten Kanal
        OverlayBridge.setSubtitle(text);
        new javax.swing.Timer((int)Math.max(250, millis), e -> {
            OverlayBridge.clearSubtitle();
            ((javax.swing.Timer)e.getSource()).stop();
        }).start();
    }
}
