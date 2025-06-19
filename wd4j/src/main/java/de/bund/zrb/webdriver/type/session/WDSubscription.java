package de.bund.zrb.webdriver.type.session;

import de.bund.zrb.webdriver.mapping.StringWrapper;

public class WDSubscription implements StringWrapper {
    private final String value;

    public WDSubscription(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Event name must not be null or empty.");
        }
        this.value = value;
    }

    public WDSubscription(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("Event name must not be null or empty.");
        }
        this.value = value.toString();
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}