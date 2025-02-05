package wd4j.impl.webdriver.type.session;

public class UnsubscribeByAttributesRequest {
    private final String eventName;

    public UnsubscribeByAttributesRequest(String eventName) {
        if (eventName == null || eventName.isEmpty()) {
            throw new IllegalArgumentException("Event name must not be null or empty.");
        }
        this.eventName = eventName;
    }

    public String getEventName() {
        return eventName;
    }
}