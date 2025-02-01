package wd4j.impl.module.type;

public class ScriptLocalValue {
    private final Object value;

    public ScriptLocalValue(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}