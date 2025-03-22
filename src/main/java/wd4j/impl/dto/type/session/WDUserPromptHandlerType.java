package wd4j.impl.dto.type.session;

import wd4j.impl.dto.mapping.EnumWrapper;

public enum WDUserPromptHandlerType implements EnumWrapper {
    ACCEPT("accept"),
    DISMISS("dismiss"),
    IGNORE("ignore");

    private final String value;

    WDUserPromptHandlerType(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}