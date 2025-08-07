package de.bund.zrb.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class GivenTypeDefinition {

    private final String type;
    private final String label;
    private final Map<String, GivenField> fields = new LinkedHashMap<>();

    public GivenTypeDefinition(String type, String label) {
        this.type = type;
        this.label = label;
    }

    public void addField(String name, String label, Object defaultValue, Class<?> type) {
        fields.put(name, new GivenField(name, label, defaultValue, type));
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public Map<String, GivenField> getFields() {
        return fields;
    }

    public static class GivenField {
        public final String name;
        public final String label;
        public final Object defaultValue;
        public final Class<?> type;

        public GivenField(String name, String label, Object defaultValue, Class<?> type) {
            this.name = name;
            this.label = label;
            this.defaultValue = defaultValue;
            this.type = type;
        }
    }
}
