package de.bund.zrb.event;

/**
 * Request to control the event logging service, optionally for a specific user (null = active UI session).
 * Similar to {@link RecordControlRequestedEvent}, this application event is published by UI
 * commands to signal the desired operation. The {@link RecorderEventBridge} listens for
 * this event and delegates to the appropriate {@link de.bund.zrb.service.RecorderCoordinator}
 * method to perform the requested operation.
 */
public final class EventServiceControlRequestedEvent implements ApplicationEvent<EventServiceControlRequestedEvent.Payload> {

    /** Immutable payload for bus delivery. */
    public static final class Payload {
        private final Operation operation;
        private final String username; // nullable

        public Payload(Operation operation, String username) {
            if (operation == null) throw new IllegalArgumentException("operation must not be null");
            this.operation = operation;
            this.username = username;
        }

        public Operation getOperation() { return operation; }
        public String getUsername() { return username; }
    }

    private final Payload payload;

    public EventServiceControlRequestedEvent(Operation operation) {
        this(operation, null);
    }

    public EventServiceControlRequestedEvent(Operation operation, String username) {
        this.payload = new Payload(operation, username);
    }

    @Override
    public Payload getPayload() {
        return payload;
    }

    /** Define event logging control operations. */
    public enum Operation {
        START, STOP, TOGGLE
    }
}