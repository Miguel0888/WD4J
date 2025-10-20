package de.bund.zrb.event;

/** Fire this after saving precond.json to refresh the preconditions tree. */
public class PreconditionSavedEvent implements ApplicationEvent {
    private final String payload; // optional

    public PreconditionSavedEvent() { this(null); }

    public PreconditionSavedEvent(String payload) { this.payload = payload; }

    @Override
    public Object getPayload() { return payload; }
}
