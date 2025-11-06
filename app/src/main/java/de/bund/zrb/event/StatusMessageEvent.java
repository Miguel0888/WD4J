package de.bund.zrb.event;

public final class StatusMessageEvent implements ApplicationEvent<StatusMessageEvent.Payload> {

    public static final class Payload {
        private final String text;
        private final int durationMs;
        private final Severity severity; // may be null

        public Payload(String text, int durationMs, Severity severity) {
            this.text = text;
            this.durationMs = durationMs <= 0 ? 3000 : durationMs;
            this.severity = severity;
        }
        public String getText() { return text; }
        public int getDurationMs() { return durationMs; }
        public Severity getSeverity() { return severity; }
    }

    private final Payload payload;

    public StatusMessageEvent(String text) { this(text, 3000, null); }
    public StatusMessageEvent(String text, int durationMs) { this(text, durationMs, null); }
    public StatusMessageEvent(String text, int durationMs, Severity severity) {
        this.payload = new Payload(text, durationMs, severity);
    }

    @Override
    public Payload getPayload() { return payload; }
}
