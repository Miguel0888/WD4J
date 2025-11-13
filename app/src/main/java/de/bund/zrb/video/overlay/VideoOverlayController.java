package de.bund.zrb.video.overlay;

import de.bund.zrb.event.ApplicationEventBus;

/** Einfacher Overlay-Controller: konsumiert Events und setzt Texte via OverlayBridge. */
public final class VideoOverlayController {

    private final VideoOverlaySink sink;

    public VideoOverlayController() {
        this(new DefaultVideoOverlaySink());
    }

    public VideoOverlayController(VideoOverlaySink sink) {
        this.sink = sink == null ? new DefaultVideoOverlaySink() : sink;
    }

    private final java.util.function.Consumer<VideoOverlayEvent> listener = new java.util.function.Consumer<VideoOverlayEvent>() {
        @Override public void accept(VideoOverlayEvent ev) {
            VideoOverlayEvent.Payload p = ev.getPayload();
            if (p == null) return;
            switch (p.kind) {
                case SUITE:
                case ROOT:
                    sink.setCaption(p.name, VideoOverlayStyle.defaultsCaption());
                    break;
                case CASE:
                    sink.setSubtitle(p.name, VideoOverlayStyle.defaultsSubtitle());
                    break;
                case ACTION:
                default:
                    // Optional: könnte z. B. kurzzeitig eingeblendet werden
                    break;
            }
        }
    };

    private boolean active;

    public synchronized void start() {
        if (active) return;
        ApplicationEventBus.getInstance().subscribe(VideoOverlayEvent.class, listener);
        active = true;
    }

    public synchronized void stop() {
        if (!active) return;
        ApplicationEventBus.getInstance().unsubscribe(VideoOverlayEvent.class, listener);
        active = false;
        sink.clearSubtitle();
        sink.clearCaption();
    }
}
