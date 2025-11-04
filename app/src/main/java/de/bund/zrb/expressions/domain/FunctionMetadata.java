package de.bund.zrb.expressions.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FunctionMetadata {

    private final String name;
    private final String description;
    private final List<String> paramNames;
    private final List<String> paramDescriptions;

    public FunctionMetadata(String name, String description, List<String> paramNames) {
        this(name, description, paramNames, null);
    }

    public FunctionMetadata(String name, String description,
                            List<String> paramNames, List<String> paramDescriptions) {
        this.name = name;
        this.description = description != null ? description : "";
        this.paramNames = paramNames != null ? new ArrayList<String>(paramNames) : Collections.<String>emptyList();
        this.paramDescriptions = paramDescriptions != null ? new ArrayList<String>(paramDescriptions) : Collections.<String>emptyList();
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    // Variant A: provider reads names from here
    public List<String> getParamNames() { return Collections.unmodifiableList(paramNames); }

    // Variant B (optional): provider reads descriptions from here
    public List<String> getParamDescriptions() { return Collections.unmodifiableList(paramDescriptions); }

    // Optional legacy: some code looks for getParameters(); return names as strings
    public List<String> getParameters() { return getParamNames(); }
}
