package de.bund.zrb.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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

    public void addField(String name, String label, Object defaultValue, Class<?> type, List<?> options) {
        fields.put(name, new GivenField(name, label, defaultValue, type, options));
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
        public final List<?> options;

        public GivenField(String name, String label, Object defaultValue, Class<?> type) {
            this(name, label, defaultValue, type, Collections.emptyList());
        }

        public GivenField(String name, String label, Object defaultValue, Class<?> type, List<?> options) {
            this.name = name;
            this.label = label;
            this.defaultValue = defaultValue;
            this.type = type;
            this.options = options != null ? options : Collections.emptyList();
        }
    }
}
