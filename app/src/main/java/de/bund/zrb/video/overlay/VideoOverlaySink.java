package de.bund.zrb.video.overlay;

/** Abstraktions-Schnittstelle, damit unterschiedliche Recorder-Libs Overlays setzen können. */
public interface VideoOverlaySink {
    void setCaption(String text, VideoOverlayStyle style);
    void clearCaption();

    void setSubtitle(String text, VideoOverlayStyle style);
    void clearSubtitle();

    /** Kurzzeitiger Hinweis für Actions (alter Kanal – nicht mehr benutzt für persistente Action). */
    default void showTransient(String text, VideoOverlayStyle style, long millis) { /* legacy noop */ }

    // NEU: Eigener Action-Kanal (dritte Box)
    void setAction(String text, VideoOverlayStyle style);
    void clearAction();

    /** Transiente Action-Nachricht auf eigener Action-Ebene. */
    default void showTransientAction(String text, VideoOverlayStyle style, long millis) { /* optional */ }
}
