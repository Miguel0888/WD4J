package wd4j.impl.webdriver.command.request.parameters.network;

import wd4j.impl.webdriver.mapping.EnumWrapper;

public enum CacheBehavior implements EnumWrapper {
    DEFAULT("default"),
    BYPASS("bypass");

    private final String value;

    CacheBehavior(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}
