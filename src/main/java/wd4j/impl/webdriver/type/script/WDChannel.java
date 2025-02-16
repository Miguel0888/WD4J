package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.mapping.StringWrapper;

public class WDChannel implements StringWrapper {
    private final String value;

    public WDChannel(String value) {
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