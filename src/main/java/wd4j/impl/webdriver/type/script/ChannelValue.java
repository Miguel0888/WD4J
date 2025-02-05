package wd4j.impl.webdriver.type.script;

public class ChannelValue {
    private final String value;

    public ChannelValue(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Value must not be null or empty.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}