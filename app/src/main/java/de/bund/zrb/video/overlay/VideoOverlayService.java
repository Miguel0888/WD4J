package de.bund.zrb.video.overlay;

import de.bund.zrb.service.SettingsService;

/**
 * Zentrale Steuerung für das Video-Overlay mit Persistenz über SettingsService.
 */
public final class VideoOverlayService {

    private static final VideoOverlayService INSTANCE = new VideoOverlayService();

    private final VideoOverlayController controller = new VideoOverlayController();
    private boolean active;

    private VideoOverlayService() { }

    public static VideoOverlayService getInstance() { return INSTANCE; }

    public synchronized boolean isEnabled() {
        Boolean b = SettingsService.getInstance().get("video.overlay.enabled", Boolean.class);
        return b == null ? true : b.booleanValue();
    }

    public synchronized void setEnabled(boolean enabled) {
        SettingsService.getInstance().set("video.overlay.enabled", enabled);
        if (enabled && !active) {
            controller.start();
            active = true;
        } else if (!enabled && active) {
            controller.stop();
            active = false;
        }
    }

    /** Starte/Stopp je nach Setting; bei App-Start aufrufen. */
    public synchronized void ensureStartedIfEnabled() {
        boolean en = isEnabled();
        if (en && !active) {
            controller.start();
            active = true;
        } else if (!en && active) {
            controller.stop();
            active = false;
        }
    }

    public VideoOverlayStyle getCaptionStyle() {
        return new VideoOverlayStyle(
                str("video.overlay.caption.fontColor", "#FFFFFF"),
                str("video.overlay.caption.bgColor", "rgba(0,0,0,0.5)"),
                intVal("video.overlay.caption.fontSize", 24)
        );
    }

    public VideoOverlayStyle getSubtitleStyle() {
        return new VideoOverlayStyle(
                str("video.overlay.subtitle.fontColor", "#FFFFFF"),
                str("video.overlay.subtitle.bgColor", "rgba(0,0,0,0.5)"),
                intVal("video.overlay.subtitle.fontSize", 20)
        );
    }

    public VideoOverlayStyle getActionStyle() {
        return new VideoOverlayStyle(
                str("video.overlay.action.fontColor", "#FFD700"),
                str("video.overlay.action.bgColor", "rgba(0,0,0,0.4)"),
                intVal("video.overlay.action.fontSize", 16)
        );
    }

    public boolean isCaptionEnabled() { return bool("video.overlay.caption.enabled", true); }
    public boolean isSubtitleEnabled() { return bool("video.overlay.subtitle.enabled", true); }
    public boolean isActionTransientEnabled() { return bool("video.overlay.action.transient.enabled", false); }
    public int getActionTransientDurationMs() { return intVal("video.overlay.action.transient.durationMs", 1500); }

    public void applyCaptionStyle(VideoOverlayStyle s) { saveStyle("caption", s); }
    public void applySubtitleStyle(VideoOverlayStyle s) { saveStyle("subtitle", s); }
    public void applyActionStyle(VideoOverlayStyle s) { saveStyle("action", s); }

    public void setCaptionEnabled(boolean b) { SettingsService.getInstance().set("video.overlay.caption.enabled", b); }
    public void setSubtitleEnabled(boolean b) { SettingsService.getInstance().set("video.overlay.subtitle.enabled", b); }
    public void setActionTransientEnabled(boolean b) { SettingsService.getInstance().set("video.overlay.action.transient.enabled", b); }
    public void setActionTransientDurationMs(int ms) { SettingsService.getInstance().set("video.overlay.action.transient.durationMs", ms); }

    private void saveStyle(String prefix, VideoOverlayStyle s) {
        if (s == null) return;
        SettingsService.getInstance().set("video.overlay."+prefix+".fontColor", s.getFontColor());
        SettingsService.getInstance().set("video.overlay."+prefix+".bgColor", s.getBackgroundColor());
        SettingsService.getInstance().set("video.overlay."+prefix+".fontSize", s.getFontSizePx());
    }

    private String str(String key, String def) {
        String v = SettingsService.getInstance().get(key, String.class);
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }
    private int intVal(String key, int def) {
        Integer v = SettingsService.getInstance().get(key, Integer.class);
        return v == null ? def : v.intValue();
    }
    private boolean bool(String key, boolean def) {
        Boolean b = SettingsService.getInstance().get(key, Boolean.class);
        return b == null ? def : b.booleanValue();
    }
}
