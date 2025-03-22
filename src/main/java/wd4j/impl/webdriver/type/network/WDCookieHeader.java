package wd4j.impl.webdriver.type.network;

// ToDo: There  might be a better implementation in params ??
public class WDCookieHeader {
    private final String name;
    private final WDBytesValue value;

    public WDCookieHeader(String name, WDBytesValue value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }
}
