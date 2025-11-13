package de.bund.zrb.video.overlay;

import de.bund.zrb.event.ApplicationEvent;

public final class VideoOverlayEvent implements ApplicationEvent<VideoOverlayEvent.Payload> {

    public enum Kind { ROOT, SUITE, CASE, ACTION }

    public static final class Payload {
        public final Kind kind;
        public final String name;      // Anzeigename (Suite/Case/Action)
        public final String details;   // optional: Beschreibung, Typ etc.
        public Payload(Kind kind, String name, String details) {
            this.kind = kind;
            this.name = name == null ? "" : name;
            this.details = details;
        }
    }

    private final Payload payload;

    public VideoOverlayEvent(Kind kind, String name) {
        this(kind, name, null);
    }
    public VideoOverlayEvent(Kind kind, String name, String details) {
        this.payload = new Payload(kind, name, details);
    }

    @Override
    public Payload getPayload() { return payload; }
}

