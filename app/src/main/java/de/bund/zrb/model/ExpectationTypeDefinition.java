package de.bund.zrb.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Repräsentiert die Konfiguration eines konkreten Erwartungstyps.
 * Z. B. Screenshot, JavaScript etc.
 */
public class ExpectationTypeDefinition {

    private final String type;
    private final String label;
    private final Map<String, ExpectationField> fields = new LinkedHashMap<>();
    private final Consumer<Map<String, Object>> validator;

    public ExpectationTypeDefinition(String type, String label,
                                     Consumer<Map<String, Object>> validator) {
        this.type = type;
        this.label = label;
        this.validator = validator;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public Map<String, ExpectationField> getFields() {
        return fields;
    }

    public void addField(String name, String label, Object defaultValue, Class<?> type) {
        fields.put(name, new ExpectationField(name, label, defaultValue, type));
    }

    public void addField(String name, String label, Object defaultValue, Class<?> type, List<?> options) {
        fields.put(name, new ExpectationField(name, label, defaultValue, type, options));
    }

    public void validate(Map<String, Object> params) throws ValidationException {
        if (validator != null) {
            validator.accept(params);
        }
    }

    public static class ExpectationField {
        public final String name;
        public final String label;
        public final Object defaultValue;
        public final Class<?> type;
        public final List<?> options;

        public ExpectationField(String name, String label, Object defaultValue, Class<?> type) {
            this(name, label, defaultValue, type, Collections.emptyList());
        }

        public ExpectationField(String name, String label, Object defaultValue, Class<?> type, List<?> options) {
            this.name = name;
            this.label = label;
            this.defaultValue = defaultValue;
            this.type = type;
            this.options = options;
        }
    }

}
