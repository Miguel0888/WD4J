package wd4j.impl.webdriver.type.session;

import wd4j.impl.webdriver.mapping.StringWrapper;

public class Subscription implements StringWrapper {
    private final String value;

    public Subscription(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Event name must not be null or empty.");
        }
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}