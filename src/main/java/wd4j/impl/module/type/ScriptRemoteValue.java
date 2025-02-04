package wd4j.impl.module.type;

import java.util.List;
import java.util.Map;

public class ScriptRemoteValue {
    private final String type;
    private final Object value;
    private final String reference;

    public ScriptRemoteValue(String type, Object value) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must not be null or empty.");
        }
        this.type = type;
        this.value = value;
        this.reference = null; // Nur für referenzierte Objekte notwendig
    }

    public ScriptRemoteValue(String type, String reference) {
        if (type == null || type.isEmpty()) {
            throw new IllegalArgumentException("Type must not be null or empty.");
        }
        if (reference == null || reference.isEmpty()) {
            throw new IllegalArgumentException("Reference must not be null or empty.");
        }
        this.type = type;
        this.value = null; // Referenzierte Objekte haben keinen direkten Wert
        this.reference = reference;
    }

    public String getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public String getReference() {
        return reference;
    }

    @Override
    public String toString() {
        return "ScriptRemoteValue{" +
                "type='" + type + '\'' +
                ", value=" + value +
                ", reference='" + reference + '\'' +
                '}';
    }

    // Statische Factory-Methoden für verschiedene RemoteValue-Typen

    public static ScriptRemoteValue primitive(Object value) {
        return new ScriptRemoteValue("primitive", value);
    }

    public static ScriptRemoteValue symbol(String reference) {
        return new ScriptRemoteValue("symbol", reference);
    }

    public static ScriptRemoteValue array(List<ScriptRemoteValue> values) {
        return new ScriptRemoteValue("array", values);
    }

    public static ScriptRemoteValue object(Map<String, ScriptRemoteValue> properties) {
        return new ScriptRemoteValue("object", properties);
    }

    public static ScriptRemoteValue function(String reference) {
        return new ScriptRemoteValue("function", reference);
    }

    public static ScriptRemoteValue regExp(String reference) {
        return new ScriptRemoteValue("regexp", reference);
    }

    public static ScriptRemoteValue date(String reference) {
        return new ScriptRemoteValue("date", reference);
    }

    public static ScriptRemoteValue map(Map<ScriptRemoteValue, ScriptRemoteValue> entries) {
        return new ScriptRemoteValue("map", entries);
    }

    public static ScriptRemoteValue set(List<ScriptRemoteValue> values) {
        return new ScriptRemoteValue("set", values);
    }

    public static ScriptRemoteValue weakMap(String reference) {
        return new ScriptRemoteValue("weakmap", reference);
    }

    public static ScriptRemoteValue weakSet(String reference) {
        return new ScriptRemoteValue("weakset", reference);
    }

    public static ScriptRemoteValue generator(String reference) {
        return new ScriptRemoteValue("generator", reference);
    }

    public static ScriptRemoteValue error(String reference) {
        return new ScriptRemoteValue("error", reference);
    }

    public static ScriptRemoteValue proxy(String reference) {
        return new ScriptRemoteValue("proxy", reference);
    }

    public static ScriptRemoteValue promise(String reference) {
        return new ScriptRemoteValue("promise", reference);
    }

    public static ScriptRemoteValue typedArray(String reference) {
        return new ScriptRemoteValue("typedarray", reference);
    }

    public static ScriptRemoteValue arrayBuffer(String reference) {
        return new ScriptRemoteValue("arraybuffer", reference);
    }

    public static ScriptRemoteValue nodeList(List<ScriptRemoteValue> values) {
        return new ScriptRemoteValue("nodelist", values);
    }

    public static ScriptRemoteValue htmlCollection(List<ScriptRemoteValue> values) {
        return new ScriptRemoteValue("htmlcollection", values);
    }

    public static ScriptRemoteValue node(String reference) {
        return new ScriptRemoteValue("node", reference);
    }

    public static ScriptRemoteValue windowProxy(String reference) {
        return new ScriptRemoteValue("windowproxy", reference);
    }
}
