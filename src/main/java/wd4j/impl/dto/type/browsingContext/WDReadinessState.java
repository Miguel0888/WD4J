package wd4j.impl.dto.type.browsingContext;

import wd4j.impl.dto.mapping.EnumWrapper;

public enum WDReadinessState implements EnumWrapper {
    NONE("none"),
    INTERACTIVE("interactive"),
    COMPLETE("complete");

    private final String value;

    WDReadinessState(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}