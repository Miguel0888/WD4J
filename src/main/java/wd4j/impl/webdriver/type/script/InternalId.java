package wd4j.impl.webdriver.type.script;

public class InternalId {
    private final String value;

    public InternalId(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("ID must not be null or empty.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}