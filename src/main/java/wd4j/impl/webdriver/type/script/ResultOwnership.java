package wd4j.impl.webdriver.type.script;

import wd4j.impl.webdriver.mapping.EnumWrapper;

public enum ResultOwnership implements EnumWrapper {
    ROOT("root"),
    NONE("none");

    private final String value;

    ResultOwnership(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
