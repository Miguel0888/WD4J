package wd4j.impl.webdriver.type.script;

public class Channel {
    private final String value;

    public Channel(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Channel must not be null or empty.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}