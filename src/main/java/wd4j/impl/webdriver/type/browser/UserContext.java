package wd4j.impl.webdriver.type.browser;

import wd4j.impl.webdriver.mapping.StringWrapper;

public class UserContext implements StringWrapper {
    private final String value;

    public UserContext(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}