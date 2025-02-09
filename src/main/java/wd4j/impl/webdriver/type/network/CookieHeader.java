package wd4j.impl.webdriver.type.network;

// ToDo: There  might be a better implementation in params ??
public class CookieHeader {
    private final String name;
    private final BytesValue value;

    public CookieHeader(String name, BytesValue value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }
}
