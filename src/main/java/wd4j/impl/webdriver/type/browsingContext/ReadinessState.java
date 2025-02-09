package wd4j.impl.webdriver.type.browsingContext;

import wd4j.impl.webdriver.mapping.EnumWrapper;

public enum ReadinessState implements EnumWrapper {
    NONE("none"),
    INTERACTIVE("interactive"),
    COMPLETE("complete");

    private final String value;

    ReadinessState(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}