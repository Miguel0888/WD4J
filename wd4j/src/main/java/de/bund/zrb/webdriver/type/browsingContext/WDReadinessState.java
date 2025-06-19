package de.bund.zrb.webdriver.type.browsingContext;

import de.bund.zrb.webdriver.mapping.EnumWrapper;

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