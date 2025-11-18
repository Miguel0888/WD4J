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

    private javax.swing.Timer suiteTimer;
    private javax.swing.Timer caseTimer;

    private final java.util.function.Consumer<VideoOverlayEvent> listener = new java.util.function.Consumer<VideoOverlayEvent>() {
        @Override public void accept(VideoOverlayEvent ev) {
            VideoOverlayEvent.Payload p = ev.getPayload();
            if (p == null) return;
            VideoOverlayService svc = VideoOverlayService.getInstance();
            switch (p.kind) {
                case SUITE:
                case ROOT: {
                    // Neue Suite blendet Suite-Overlay ein; Dauer berücksichtigen
                    if (suiteTimer != null) { suiteTimer.stop(); suiteTimer = null; }
                    if (svc.isCaptionEnabled()) {
                        sink.setCaption(p.name, svc.getCaptionStyle());
                        long dur = svc.getSuiteDisplayDurationMs();
                        long special = svc.getInfinityMarkerMs();
                        if (dur > 0 && dur < special) {
                            suiteTimer = new javax.swing.Timer((int) dur, e -> { sink.clearCaption(); suiteTimer.stop(); });
                            suiteTimer.setRepeats(false);
                            suiteTimer.start();
                        }
                    } else {
                        sink.clearCaption();
                    }
                    break;
                }
                case CASE: {
                    if (caseTimer != null) { caseTimer.stop(); caseTimer = null; }
                    if (svc.isSubtitleEnabled()) {
                        sink.setSubtitle(p.name, svc.getSubtitleStyle());
                        long dur = svc.getCaseDisplayDurationMs();
                        long special = svc.getInfinityMarkerMs();
                        if (dur > 0 && dur < special) {
                            caseTimer = new javax.swing.Timer((int) dur, e -> { sink.clearSubtitle(); caseTimer.stop(); });
                            caseTimer.setRepeats(false);
                            caseTimer.start();
                        }
                    } else {
                        sink.clearSubtitle();
                    }
                    break;
                }
                case ACTION: {
                    // Transient auf Subtitle-Kanal – Dauer aus Service
                    if (svc.isActionTransientEnabled()) {
                        long dur = svc.getActionTransientDurationMs();
                        long special = svc.getInfinityMarkerMs();
                        if (dur >= special) {
                            // Bis zum nächsten Action-Event sichtbar lassen
                            sink.setSubtitle(p.name, svc.getActionStyle());
                        } else {
                            sink.showTransient(p.name, svc.getActionStyle(), dur);
                        }
                    }
                    break;
                }
                default: break;
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
        if (suiteTimer != null) { suiteTimer.stop(); suiteTimer = null; }
        if (caseTimer != null) { caseTimer.stop(); caseTimer = null; }
        sink.clearSubtitle();
        sink.clearCaption();
    }
}
