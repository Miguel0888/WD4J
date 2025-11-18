package de.bund.zrb.video;

import java.util.Objects;

/** Dünne, null-sichere Bridge zu einem (ggf.) aktiven WindowRecorder. */
public final class OverlayBridge {

    /** Optionaler Provider, falls ihr selbst festlegt, welcher Recorder aktiv ist. */
    public interface Provider {
        Object getActiveRecorder(); // kann WindowRecorder oder JcodecWindowRecorder sein
    }

    private static volatile Provider provider;

    private OverlayBridge() {}

    public static void setProvider(Provider p) { provider = p; }

    private static Object resolve() {
        Provider p = provider;
        if (p != null) {
            try {
                Object r = p.getActiveRecorder();
                if (r != null) return r;
            } catch (Throwable ignore) {}
        }
        try {
            WindowRecorder r = WindowRecorder.getCurrentActive();
            if (r != null) return r;
        } catch (Throwable ignore) {}
        try {
            JcodecWindowRecorder r2 = JcodecWindowRecorder.getCurrentActive();
            if (r2 != null) return r2;
        } catch (Throwable ignore) {}
        return null;
    }

    // -------- Caption (oben) ----------
    public static void setCaption(String text) {
        Object r = resolve();
        if (r instanceof WindowRecorder) {
            WindowRecorder wr = (WindowRecorder) r;
            wr.setCaptionVisible(text != null && !text.isEmpty());
            wr.setCaptionText(text == null ? "" : text);
        } else if (r instanceof JcodecWindowRecorder) {
            JcodecWindowRecorder jr = (JcodecWindowRecorder) r;
            jr.setCaptionVisible(text != null && !text.isEmpty());
            jr.setCaptionText(text == null ? "" : text);
        }
    }
    public static void clearCaption() { setCaption(""); }

    /** Stil für Caption setzen. */
    public static void setCaptionStyle(WindowRecorder.OverlayStyle style) {
        if (style == null) return;
        Object r = resolve();
        if (r instanceof WindowRecorder) {
            ((WindowRecorder) r).setCaptionStyle(style);
        } else if (r instanceof JcodecWindowRecorder) {
            ((JcodecWindowRecorder) r).setCaptionStyle(style);
        }
    }

    // -------- Subtitle (unten) --------
    public static void setSubtitle(String text) {
        Object r = resolve();
        if (r instanceof WindowRecorder) {
            WindowRecorder wr = (WindowRecorder) r;
            wr.setSubtitleVisible(text != null && !text.isEmpty());
            wr.setSubtitleText(text == null ? "" : text);
        } else if (r instanceof JcodecWindowRecorder) {
            JcodecWindowRecorder jr = (JcodecWindowRecorder) r;
            jr.setSubtitleVisible(text != null && !text.isEmpty());
            jr.setSubtitleText(text == null ? "" : text);
        }
    }
    public static void clearSubtitle() { setSubtitle(""); }

    /** Stil für Subtitle setzen. */
    public static void setSubtitleStyle(WindowRecorder.OverlayStyle style) {
        if (style == null) return;
        Object r = resolve();
        if (r instanceof WindowRecorder) {
            ((WindowRecorder) r).setSubtitleStyle(style);
        } else if (r instanceof JcodecWindowRecorder) {
            ((JcodecWindowRecorder) r).setSubtitleStyle(style);
        }
    }

    // -------- Action (eigene Ebene, frei positionierbar) --------
    public static void setAction(String text) {
        Object r = resolve();
        if (r instanceof WindowRecorder) {
            WindowRecorder wr = (WindowRecorder) r;
            wr.setActionVisible(text != null && !text.isEmpty());
            wr.setActionText(text == null ? "" : text);
        } else if (r instanceof JcodecWindowRecorder) {
            JcodecWindowRecorder jr = (JcodecWindowRecorder) r;
            jr.setActionVisible(text != null && !text.isEmpty());
            jr.setActionText(text == null ? "" : text);
        }
    }
    public static void clearAction() { setAction(""); }

    /** Stil für Action setzen. */
    public static void setActionStyle(WindowRecorder.OverlayStyle style) {
        if (style == null) return;
        Object r = resolve();
        if (r instanceof WindowRecorder) {
            ((WindowRecorder) r).setActionStyle(style);
        } else if (r instanceof JcodecWindowRecorder) {
            ((JcodecWindowRecorder) r).setActionStyle(style);
        }
    }
}
