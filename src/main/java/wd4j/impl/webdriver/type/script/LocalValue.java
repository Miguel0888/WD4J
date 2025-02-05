package wd4j.impl.webdriver.type.script;

public class LocalValue {
    private final Object value;

    public LocalValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}