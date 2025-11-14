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
}

