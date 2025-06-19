package de.bund.zrb.impl.webdriver.type.browsingContext;

import de.bund.zrb.impl.webdriver.mapping.EnumWrapper;

public enum WDUserPromptType implements EnumWrapper {
    ALERT("alert"),
    BEFOREUNLOAD("beforeunload"),
    CONFIRM("confirm"),
    PROMPT("prompt");

    private final String value;

    WDUserPromptType(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}