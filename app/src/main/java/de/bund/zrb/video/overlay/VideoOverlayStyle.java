package de.bund.zrb.video.overlay;

/** Beschreibt Stil-Optionen für Video-Overlays (Farbe, Größe, Transparenz). */
public final class VideoOverlayStyle {
    /** CSS-ähnliche Farbangabe, z. B. "#FFFFFF" oder "rgba(255,255,255,1)". */
    private final String fontColor;
    /** Hintergrundfarbe inkl. Alpha (z. B. halbtransparentes Schwarz). */
    private final String backgroundColor;
    /** Schriftgröße in px (nur informativ, Default-Sink kann dies ignorieren). */
    private final int fontSizePx;

    public VideoOverlayStyle(String fontColor, String backgroundColor, int fontSizePx) {
        this.fontColor = fontColor;
        this.backgroundColor = backgroundColor;
        this.fontSizePx = fontSizePx;
    }

    public static VideoOverlayStyle defaultsCaption() {
        return new VideoOverlayStyle("#FFFFFF", "rgba(0,0,0,0.5)", 24);
    }

    public static VideoOverlayStyle defaultsSubtitle() {
        return new VideoOverlayStyle("#FFFFFF", "rgba(0,0,0,0.5)", 20);
    }

    public String getFontColor() { return fontColor; }
    public String getBackgroundColor() { return backgroundColor; }
    public int getFontSizePx() { return fontSizePx; }
}

