package de.bund.zrb.webdriver.type.network;

public class WDHeader {
    private final String name;
    private final WDBytesValue value;

    public WDHeader(String name, WDBytesValue value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public WDBytesValue getValue() {
        return value;
    }
}