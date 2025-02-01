package wd4j.impl.module.type;

public class SessionSubscription {
    private final String eventName;

    public SessionSubscription(String eventName) {
        if (eventName == null || eventName.isEmpty()) {
            throw new IllegalArgumentException("Event name must not be null or empty.");
        }
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}