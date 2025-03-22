package wd4j.impl.webdriver.type.network;

import wd4j.impl.webdriver.mapping.EnumWrapper;

public enum WDSameSite implements EnumWrapper {
    STRICT("strict"),
    LAX("lax"),
    NONE("none");

    private final String value;

    private WDSameSite(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
