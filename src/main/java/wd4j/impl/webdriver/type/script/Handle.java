package wd4j.impl.webdriver.type.script;

public class Handle {
    private final String value;

    public Handle(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Handle must not be null or empty.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}