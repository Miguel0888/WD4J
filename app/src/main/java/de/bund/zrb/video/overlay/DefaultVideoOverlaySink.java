package de.bund.zrb.video.overlay;

import de.bund.zrb.video.OverlayBridge;
import de.bund.zrb.video.WindowRecorder;

import java.awt.*;

/** Default-Sink: delegiert an OverlayBridge und setzt Stile inkl. Positionen. */
public final class DefaultVideoOverlaySink implements VideoOverlaySink {
    @Override
    public void setCaption(String text, VideoOverlayStyle style) {
        WindowRecorder.OverlayStyle st = toOverlayStyle(style, true, "video.overlay.caption");
        if (st != null) OverlayBridge.setCaptionStyle(st);
        OverlayBridge.setCaption(text);
    }

    @Override
    public void clearCaption() { OverlayBridge.clearCaption(); }

    @Override
    public void setSubtitle(String text, VideoOverlayStyle style) {
        WindowRecorder.OverlayStyle st = toOverlayStyle(style, false, "video.overlay.subtitle");
        if (st != null) OverlayBridge.setSubtitleStyle(st);
        OverlayBridge.setSubtitle(text);
    }

    @Override
    public void clearSubtitle() { OverlayBridge.clearSubtitle(); }

    @Override
    public void showTransient(String text, VideoOverlayStyle style, long millis) {
        if (text == null || text.trim().isEmpty()) return;
        WindowRecorder.OverlayStyle st = toOverlayStyle(style, false, "video.overlay.action");
        if (st != null) OverlayBridge.setActionStyle(st);
        OverlayBridge.setAction(text);
        new javax.swing.Timer((int) Math.max(250, millis), e -> {
            OverlayBridge.clearAction();
            ((javax.swing.Timer) e.getSource()).stop();
        }).start();
    }

    private static WindowRecorder.OverlayStyle toOverlayStyle(VideoOverlayStyle s, boolean isCaption, String prefix) {
        if (s == null) return null;
        // Farben parsen
        Color font = parseColor(s.getFontColor(), Color.WHITE);
        ParsedBg bg = parseBg(s.getBackgroundColor());
        int fontPt = Math.max(8, toPointSize(s.getFontSizePx()));
        WindowRecorder.OverlayStyle base = isCaption ? WindowRecorder.OverlayStyle.defaultCaption() : WindowRecorder.OverlayStyle.defaultSubtitle();
        WindowRecorder.OverlayStyle.Builder b = new WindowRecorder.OverlayStyle.Builder(base)
                .font("SansSerif", Font.BOLD, fontPt)
                .text(font, true)
                .box(bg.color, bg.alpha);
        // Prozent-Position aus Settings holen (0..1) – falls vorhanden, anwenden
        Double posX = de.bund.zrb.service.SettingsService.getInstance().get(prefix + ".posX", Double.class);
        Double posY = de.bund.zrb.service.SettingsService.getInstance().get(prefix + ".posY", Double.class);
        if (posX != null && posY != null) {
            b.positionPercent(posX, posY);
        }
        return b.build();
    }

    private static int toPointSize(int px) { return (int) Math.max(8, Math.round(px / 1.333f)); }

    private static Color parseColor(String css, Color def) {
        if (css == null) return def;
        try {
            String s = css.trim().toLowerCase();
            if (s.startsWith("#")) return Color.decode(s);
            else if (s.startsWith("rgba")) {
                int a = s.indexOf('('), b = s.indexOf(')');
                String[] p = s.substring(a + 1, b).split(",");
                int r = Integer.parseInt(p[0].trim());
                int g = Integer.parseInt(p[1].trim());
                int bl = Integer.parseInt(p[2].trim());
                return new Color(r, g, bl);
            }
        } catch (Throwable ignore) {}
        return def;
    }

    private static final class ParsedBg { final Color color; final float alpha; ParsedBg(Color c, float a){ this.color=c; this.alpha=a; } }

    private static ParsedBg parseBg(String css) {
        if (css == null) return new ParsedBg(new Color(0, 0, 0), 0.5f);
        try {
            String s = css.trim().toLowerCase();
            if (s.startsWith("rgba")) {
                int a = s.indexOf('('), b = s.indexOf(')');
                String[] p = s.substring(a + 1, b).split(",");
                int r = Integer.parseInt(p[0].trim());
                int g = Integer.parseInt(p[1].trim());
                int bl = Integer.parseInt(p[2].trim());
                float af = Float.parseFloat(p[3].trim());
                float boxAlpha = Math.max(0f, Math.min(1f, 1f - af));
                return new ParsedBg(new Color(r, g, bl), boxAlpha);
            } else if (s.startsWith("#")) {
                return new ParsedBg(Color.decode(s), 0.5f);
            }
        } catch (Throwable ignore) {}
        return new ParsedBg(new Color(0, 0, 0), 0.5f);
    }
}
