package de.bund.zrb.video.overlay;

/** Abstraktions-Schnittstelle, damit unterschiedliche Recorder-Libs Overlays setzen können. */
public interface VideoOverlaySink {
    void setCaption(String text, VideoOverlayStyle style);
    void clearCaption();

    void setSubtitle(String text, VideoOverlayStyle style);
    void clearSubtitle();

    /** Optional: kurzzeitiger Hinweis (z. B. für Actions). Default-Implementierung kann noop sein. */
    default void showTransient(String text, VideoOverlayStyle style, long millis) { /* noop */ }
}

