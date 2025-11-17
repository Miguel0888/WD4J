package de.bund.zrb.event;

import java.time.Instant;

/** Event wird publiziert, wenn eine Suite, ein Case, eine Action oder Root gespeichert wurde. */
public final class SavedEntityEvent implements ApplicationEvent<SavedEntityEvent.Payload> {
    public static final class Payload {
        public final String entityType; // Root | Suite | Case | Action
        public final String name;       // Anzeigename
        public final String id;         // UUID falls vorhanden
        public final Instant timestamp; // Zeitpunkt des Speicherns
        public Payload(String entityType, String name, String id, Instant ts) {
            this.entityType = entityType;
            this.name = name;
            this.id = id;
            this.timestamp = ts;
        }
    }
    private final Payload payload;
    public SavedEntityEvent(Payload payload) { this.payload = payload; }
    @Override public Payload getPayload() { return payload; }
}
