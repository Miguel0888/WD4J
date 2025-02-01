package wd4j.impl.module.type;

public class SessionUnsubscribeByAttributesRequest {
    private final String eventName;

    public SessionUnsubscribeByAttributesRequest(String eventName) {
        if (eventName == null || eventName.isEmpty()) {
            throw new IllegalArgumentException("Event name must not be null or empty.");
        }
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}