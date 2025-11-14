package de.bund.zrb.ui.commands;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.Severity;
import de.bund.zrb.event.StatusMessageEvent;
import de.bund.zrb.ui.commandframework.ShortcutMenuCommand;
import de.bund.zrb.video.overlay.VideoOverlayController;

/**
 * Toggle command to enable/disable the on-screen video overlay (suite/case captions).
 * ID: "video.overlay".
 */
public class ToggleVideoOverlayCommand extends ShortcutMenuCommand {

    private volatile boolean overlayActive;
    private VideoOverlayController controller;

    @Override
    public String getId() { return "video.overlay"; }

    @Override
    public String getLabel() { return "Overlay ein-/ausschalten"; }

    @Override
    public void perform() {
        try {
            if (controller == null) controller = new VideoOverlayController();
            if (!overlayActive) {
                controller.start();
                overlayActive = true;
                ApplicationEventBus.getInstance().publish(new StatusMessageEvent("🎬 Overlay an", 2000));
            } else {
                controller.stop();
                overlayActive = false;
                ApplicationEventBus.getInstance().publish(new StatusMessageEvent("🎬 Overlay aus", 2000));
            }
        } catch (Exception ex) {
            ApplicationEventBus.getInstance().publish(new StatusMessageEvent(
                    "Overlay konnte nicht umgeschaltet werden: " + ex.getMessage(), 4000, Severity.ERROR));
        }
    }
}

