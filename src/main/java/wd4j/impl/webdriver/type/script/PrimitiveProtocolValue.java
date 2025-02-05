package wd4j.impl.webdriver.type.script;

public class PrimitiveProtocolValue {
    private final Object value;

    public PrimitiveProtocolValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}