package wd4j.impl.webdriver.type.network;

import wd4j.impl.webdriver.mapping.StringWrapper;

public class Intercept implements StringWrapper {
    private final String value;

    public Intercept(String value) {
        this.value = value;
    }

    @Override // confirmed
    public String value() {
        return value;
    }
}