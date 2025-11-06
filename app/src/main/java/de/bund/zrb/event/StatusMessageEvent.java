package de.bund.zrb.event;

public final class StatusMessageEvent implements ApplicationEvent<StatusMessageEvent.Payload> {

    public static final class Payload {
        private final String text;
        private final int durationMs;

        public Payload(String text, int durationMs) {
            this.text = text;
            this.durationMs = durationMs <= 0 ? 3000 : durationMs;
        }
        public String getText() { return text; }
        public int getDurationMs() { return durationMs; }
    }

    private final Payload payload;

    public StatusMessageEvent(String text) { this(text, 3000); }

    public StatusMessageEvent(String text, int durationMs) {
        this.payload = new Payload(text, durationMs);
    }

    @Override
    public Payload getPayload() { return payload; }
}
