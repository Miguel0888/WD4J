package wd4j.impl.webdriver.type.browsingContext;

public class BrowsingContext {
    private final String value;

    public BrowsingContext(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Context ID must not be null or empty.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
