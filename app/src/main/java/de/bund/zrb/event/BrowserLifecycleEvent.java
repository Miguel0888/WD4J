package de.bund.zrb.event;

/**
 * UI-unabhängiges Event für Browser-Statusmeldungen (Start/Stop/Fehler),
 * damit StatusBar-Updates zentral vom Service ausgelöst werden können.
 */
public final class BrowserLifecycleEvent implements ApplicationEvent<BrowserLifecycleEvent.Payload> {

    public enum Kind { STARTING, STARTED, STOPPING, STOPPED, ERROR, EXTERNALLY_CLOSED }

    public static final class Payload {
        private final Kind kind;
        private final String message;
        private final Throwable error;
        public Payload(Kind kind, String message) { this(kind, message, null); }
        public Payload(Kind kind, String message, Throwable error) {
            this.kind = kind; this.message = message; this.error = error;
        }
        public Kind getKind() { return kind; }
        public String getMessage() { return message; }
        public Throwable getError() { return error; }
    }

    private final Payload payload;
    public BrowserLifecycleEvent(Payload payload) { this.payload = payload; }
    @Override public Payload getPayload() { return payload; }
}
