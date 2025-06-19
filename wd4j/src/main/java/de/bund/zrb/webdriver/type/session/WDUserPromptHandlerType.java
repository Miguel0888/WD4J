package de.bund.zrb.webdriver.type.session;

import de.bund.zrb.webdriver.mapping.EnumWrapper;

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