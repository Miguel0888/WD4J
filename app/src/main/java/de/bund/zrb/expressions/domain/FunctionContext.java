package de.bund.zrb.expressions.domain;

import java.util.Map;

public interface FunctionContext {
    // Provide variables, services, and helpers
    String resolveVariable(String name);
    Map<String, Object> services();
}
