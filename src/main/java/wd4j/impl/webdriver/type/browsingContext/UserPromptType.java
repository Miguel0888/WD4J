package wd4j.impl.webdriver.type.browsingContext;

import wd4j.impl.webdriver.mapping.EnumWrapper;

public enum UserPromptType implements EnumWrapper {
    ALERT("alert"),
    BEFOREUNLOAD("beforeunload"),
    CONFIRM("confirm"),
    PROMPT("prompt");

    private final String value;

    UserPromptType(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}