package de.bund.zrb.video;

import java.util.Objects;

/** Dünne, null-sichere Bridge zu einem (ggf.) aktiven WindowRecorder. */
public final class OverlayBridge {

    /** Optionaler Provider, falls ihr selbst festlegt, welcher Recorder aktiv ist. */
    public interface Provider {
        WindowRecorder getActiveRecorder(); // darf null liefern
    }

    private static volatile Provider provider;

    private OverlayBridge() {}

    public static void setProvider(Provider p) { provider = p; }

    private static WindowRecorder resolve() {
        // 1) externer Provider?
        Provider p = provider;
        if (p != null) {
            try {
                WindowRecorder r = p.getActiveRecorder();
                if (r != null) return r;
            } catch (Throwable ignore) {}
        }
        // 2) Fallback auf WindowRecorder.CURRENT (s. Abschnitt 2 unten)
        try {
            return WindowRecorder.getCurrentActive();
        } catch (Throwable ignore) {
            return null;
        }
    }

    // -------- Caption (oben) ----------
    public static void setCaption(String text) {
        WindowRecorder r = resolve();
        if (r != null) {
            r.setCaptionVisible(text != null && !text.isEmpty());
            r.setCaptionText(text == null ? "" : text);
        }
    }
    public static void clearCaption() { setCaption(""); }

    // -------- Subtitle (unten) --------
    public static void setSubtitle(String text) {
        WindowRecorder r = resolve();
        if (r != null) {
            r.setSubtitleVisible(text != null && !text.isEmpty());
            r.setSubtitleText(text == null ? "" : text);
        }
    }
    public static void clearSubtitle() { setSubtitle(""); }
}
