package wd4j.impl.webdriver.type.network;

import wd4j.impl.webdriver.mapping.StringWrapper;

public class WDIntercept implements StringWrapper {
    private final String value;

    public WDIntercept(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}