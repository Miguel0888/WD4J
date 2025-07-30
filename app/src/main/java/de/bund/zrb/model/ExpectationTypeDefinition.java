package de.bund.zrb.model;

import java.util.LinkedHashMap;
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

    public void addField(String name, String label, String defaultValue) {
        fields.put(name, new ExpectationField(name, label, defaultValue));
    }

    public void validate(Map<String, Object> params) throws ValidationException {
        if (validator != null) {
            validator.accept(params);
        }
    }

    public static class ExpectationField {
        public final String name;
        public final String label;
        public final String defaultValue;

        public ExpectationField(String name, String label, String defaultValue) {
            this.name = name;
            this.label = label;
            this.defaultValue = defaultValue;
        }
    }
}
