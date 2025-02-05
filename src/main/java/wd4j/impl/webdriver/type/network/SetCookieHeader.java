package wd4j.impl.webdriver.type.network;

public class SetCookieHeader {
    private final String name;
    private final String value;

    public SetCookieHeader(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
