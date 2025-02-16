package wd4j.impl.webdriver.type.log;

import wd4j.impl.webdriver.mapping.EnumWrapper;

public enum WDLevel implements EnumWrapper {
    DEBUG("debug"),
    INFO("info"),
    WARN("warn"),
    ERROR("error");

    private final String value;

    WDLevel(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
