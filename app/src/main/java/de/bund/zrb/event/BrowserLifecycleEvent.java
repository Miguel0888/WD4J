package de.bund.zrb.event;

/**
 * UI-unabhängiges Event für Browser-Statusmeldungen (Start/Stop/Fehler),
 * damit StatusBar-Updates zentral vom Service ausgelöst werden können.
 */
public final class BrowserLifecycleEvent implements ApplicationEvent<BrowserLifecycleEvent.Payload> {

    public enum Kind { STARTING, STARTED, STOPPING, STOPPED, ERROR, EXTERNALLY_CLOSED }

    public static final class Action {
        private final String label;
        private final Runnable runnable;
        public Action(String label, Runnable runnable) { this.label = label; this.runnable = runnable; }
        public String getLabel() { return label; }
        public Runnable getRunnable() { return runnable; }
    }

    public static final class Payload {
        private final Kind kind;
        private final String message;
        private final Throwable error;
        private final Action action; // optional
        public Payload(Kind kind, String message) { this(kind, message, null, null); }
        public Payload(Kind kind, String message, Throwable error) { this(kind, message, error, null); }
        public Payload(Kind kind, String message, Action action) { this(kind, message, null, action); }
        public Payload(Kind kind, String message, Throwable error, Action action) {
            this.kind = kind; this.message = message; this.error = error; this.action = action;
        }
        public Kind getKind() { return kind; }
        public String getMessage() { return message; }
        public Throwable getError() { return error; }
        public Action getAction() { return action; }
    }

    private final Payload payload;
    public BrowserLifecycleEvent(Payload payload) { this.payload = payload; }
    @Override public Payload getPayload() { return payload; }
}
