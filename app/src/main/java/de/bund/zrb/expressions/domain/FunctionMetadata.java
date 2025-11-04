package de.bund.zrb.expressions.domain;

import java.util.Collections;
import java.util.List;

public final class FunctionMetadata {
    private final String name;
    private final String description;
    private final List<String> parameterNames;
    private final List<String> parameterDescriptions;

    public FunctionMetadata(String name, String description, List<String> parameterNames, List<String> parameterDescriptions) {
        this.name = name;
        this.description = description;
        this.parameterNames = parameterNames == null ? Collections.<String>emptyList() : parameterNames;
        this.parameterDescriptions = parameterDescriptions;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getParameterNames() { return parameterNames; }
    public List<String> getParameterDescriptions() { return parameterDescriptions; }
}
