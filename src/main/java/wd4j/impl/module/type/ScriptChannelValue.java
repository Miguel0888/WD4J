package wd4j.impl.module.type;

public class ScriptChannelValue {
    private final String value;

    public ScriptChannelValue(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("Value must not be null or empty.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}