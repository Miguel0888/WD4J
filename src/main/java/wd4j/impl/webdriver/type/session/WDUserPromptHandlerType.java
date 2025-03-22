package wd4j.impl.webdriver.type.session;

import wd4j.impl.webdriver.mapping.EnumWrapper;

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