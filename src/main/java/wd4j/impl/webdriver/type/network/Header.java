package wd4j.impl.webdriver.type.network;

public class Header {
    private final String name;
    private final BytesValue value;

    public Header(String name, BytesValue value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public BytesValue getValue() {
        return value;
    }
}