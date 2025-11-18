package de.bund.zrb.video.overlay;

import de.bund.zrb.video.OverlayBridge;
import de.bund.zrb.video.WindowRecorder;
import de.bund.zrb.service.SettingsService;

import java.awt.*;

/** Default-Sink: delegiert an OverlayBridge und setzt Stile inkl. Positionen für drei getrennte Boxen. */
public final class DefaultVideoOverlaySink implements VideoOverlaySink {
    @Override
    public void setCaption(String text, VideoOverlayStyle style) {
        if (style != null) OverlayBridge.setCaptionStyle(toOverlayStyle(style, true, "video.overlay.caption"));
        OverlayBridge.setCaption(text);
    }
    @Override public void clearCaption() { OverlayBridge.clearCaption(); }

    @Override
    public void setSubtitle(String text, VideoOverlayStyle style) {
        if (style != null) OverlayBridge.setSubtitleStyle(toOverlayStyle(style, false, "video.overlay.subtitle"));
        OverlayBridge.setSubtitle(text);
    }
    @Override public void clearSubtitle() { OverlayBridge.clearSubtitle(); }

    @Override
    public void setAction(String text, VideoOverlayStyle style) {
        if (style != null) OverlayBridge.setActionStyle(toOverlayStyle(style, false, "video.overlay.action"));
        OverlayBridge.setAction(text);
    }
    @Override public void clearAction() { OverlayBridge.clearAction(); }

    @Override
    public void showTransientAction(String text, VideoOverlayStyle style, long millis) {
        if (text == null || text.trim().isEmpty()) return;
        if (style != null) OverlayBridge.setActionStyle(toOverlayStyle(style, false, "video.overlay.action"));
        OverlayBridge.setAction(text);
        new javax.swing.Timer((int) Math.max(250, millis), e -> {
            OverlayBridge.clearAction();
            ((javax.swing.Timer) e.getSource()).stop();
        }).start();
    }

    private static WindowRecorder.OverlayStyle toOverlayStyle(VideoOverlayStyle s, boolean isCaption, String prefix) {
        Color font = parseColor(s.getFontColor(), Color.WHITE);
        ParsedBg bg = parseBg(s.getBackgroundColor());
        int fontPt = Math.max(8, toPointSize(s.getFontSizePx()));
        WindowRecorder.OverlayStyle base = isCaption ? WindowRecorder.OverlayStyle.defaultCaption() : WindowRecorder.OverlayStyle.defaultSubtitle();
        WindowRecorder.OverlayStyle.Builder b = new WindowRecorder.OverlayStyle.Builder(base)
                .font("SansSerif", Font.PLAIN, fontPt) // Font plain; Bold war default, jetzt konfigurierbar falls benötigt
                .text(font, true)
                .box(bg.color, bg.alpha);
        Double posX = SettingsService.getInstance().get(prefix + ".posX", Double.class);
        Double posY = SettingsService.getInstance().get(prefix + ".posY", Double.class);
        if (posX != null && posY != null) b.positionPercent(posX, posY);
        return b.build();
    }

    private static int toPointSize(int px) { return (int) Math.max(8, Math.round(px / 1.333f)); }
    private static Color parseColor(String css, Color def) {
        if (css == null) return def;
        try {
            String s = css.trim().toLowerCase();
            if (s.startsWith("#")) return Color.decode(s);
            if (s.startsWith("rgba")) {
                int a = s.indexOf('('), b = s.indexOf(')');
                String[] p = s.substring(a + 1, b).split(",");
                return new Color(Integer.parseInt(p[0].trim()), Integer.parseInt(p[1].trim()), Integer.parseInt(p[2].trim()));
            }
        } catch (Throwable ignore) {}
        return def;
    }
    private static final class ParsedBg { final Color color; final float alpha; ParsedBg(Color c, float a){ color=c; alpha=a; } }
    private static ParsedBg parseBg(String css) {
        if (css == null) return new ParsedBg(new Color(0,0,0), .5f);
        try {
            String s = css.trim().toLowerCase();
            if (s.startsWith("rgba")) {
                int a = s.indexOf('('), b = s.indexOf(')');
                String[] p = s.substring(a + 1, b).split(",");
                int r = Integer.parseInt(p[0].trim());
                int g = Integer.parseInt(p[1].trim());
                int bl = Integer.parseInt(p[2].trim());
                float af = Float.parseFloat(p[3].trim()); // 0 deckend, 1 transparent
                float boxAlpha = Math.max(0f, Math.min(1f, 1f - af));
                return new ParsedBg(new Color(r,g,bl), boxAlpha);
            } else if (s.startsWith("#")) {
                return new ParsedBg(Color.decode(s), .5f);
            }
        } catch (Throwable ignore) {}
        return new ParsedBg(new Color(0,0,0), .5f);
    }
}
