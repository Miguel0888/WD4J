package de.bund.zrb.model;

import java.util.Collections;
import java.util.List;

public class ExpectationParameter {
    private final String name;
    private final String label;
    private final ParameterType type;
    private final String defaultValue;
    private final List<String> options;

    public enum ParameterType { STRING, BOOLEAN, SELECT, CODE }

    public ExpectationParameter(String name, String label, ParameterType type, String defaultValue, List<String> options) {
        this.name = name;
        this.label = label;
        this.type = type;
        this.defaultValue = defaultValue;
        this.options = options != null ? options : Collections.emptyList();
    }

    public String getName() { return name; }
    public String getLabel() { return label; }
    public ParameterType getType() { return type; }
    public String getDefaultValue() { return defaultValue; }
    public List<String> getOptions() { return options; }
}
