package wd4j.impl.module.type;

public class ScriptPrimitiveProtocolValue {
    private final Object value;

    public ScriptPrimitiveProtocolValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}