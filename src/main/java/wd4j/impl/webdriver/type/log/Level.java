package wd4j.impl.webdriver.type.log;

import wd4j.impl.webdriver.mapping.EnumWrapper;

public enum Level implements EnumWrapper {
    DEBUG("debug"),
    INFO("info"),
    WARN("warn"),
    ERROR("error");

    private final String value;

    Level(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
