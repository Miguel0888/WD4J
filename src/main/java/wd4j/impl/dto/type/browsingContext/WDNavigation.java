package wd4j.impl.dto.type.browsingContext;

import wd4j.impl.dto.mapping.StringWrapper;

public class WDNavigation implements StringWrapper {
    private final String value;

    public WDNavigation(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}