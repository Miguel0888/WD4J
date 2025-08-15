package de.bund.zrb.event;

/** Request to control recording, optionally for a specific user (null = aktive UI-Session). */
public final class RecordControlRequestedEvent implements ApplicationEvent<RecordControlRequestedEvent.Payload> {

    /** Immutable payload for bus delivery. */
    public static final class Payload {
        private final RecordOperation operation;
        private final String username; // nullable

        public Payload(RecordOperation operation, String username) {
            if (operation == null) throw new IllegalArgumentException("operation must not be null");
            this.operation = operation;
            this.username = username;
        }

        public RecordOperation getOperation() { return operation; }
        public String getUsername() { return username; }
    }

    private final Payload payload;

    public RecordControlRequestedEvent(RecordOperation operation) {
        this(operation, null);
    }

    public RecordControlRequestedEvent(RecordOperation operation, String username) {
        this.payload = new Payload(operation, username);
    }

    @Override
    public Payload getPayload() {
        return payload;
    }

    /** Define recording control operations. */
    public enum RecordOperation {
        START, STOP, TOGGLE
    }
}
