package wd4j.impl.webdriver.type.session;

public class Subscription {
    private final String value;

    public Subscription(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Event name must not be null or empty.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}