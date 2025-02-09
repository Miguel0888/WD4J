package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.mapping.StringWrapper;

public class Channel implements StringWrapper {
    private final String value;

    public Channel(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Channel must not be null or empty.");
        }
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}