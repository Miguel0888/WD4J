package wd4j.impl.webdriver.type.browser;

public class ClientWindow {
    private final String value;

    public ClientWindow(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}