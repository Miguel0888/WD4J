package wd4j.impl.webdriver.type.browsingContext;

import wd4j.impl.webdriver.mapping.StringWrapper;

public class Navigation implements StringWrapper {
    private final String value;

    public Navigation(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}