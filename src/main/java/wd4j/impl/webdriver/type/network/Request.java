package wd4j.impl.webdriver.type.network;

import wd4j.impl.webdriver.mapping.StringWrapper;

public class Request implements StringWrapper {
    private final String value;

    public Request(String id) {
        this.value = id;
    }

    @Override // confirmed
    public String value() {
        return value;
    }

}