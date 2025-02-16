package wd4j.impl.webdriver.type.network;

import wd4j.impl.webdriver.mapping.StringWrapper;

public class WDRequest implements StringWrapper {
    private final String value;

    public WDRequest(String id) {
        this.value = id;
    }

    @Override // confirmed
    public String value() {
        return value;
    }

}