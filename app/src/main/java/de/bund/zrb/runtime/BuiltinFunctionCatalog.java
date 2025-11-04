// src/main/java/de/bund/zrb/runtime/BuiltinFunctionCatalog.java
package de.bund.zrb.runtime;

import de.bund.zrb.expressions.builtins.EchoFunction;
import de.bund.zrb.expressions.builtins.DateFunction;
import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionMetadata;

import java.util.*;

public final class BuiltinFunctionCatalog {

    private final Map<String, ExpressionFunction> byName = new LinkedHashMap<String, ExpressionFunction>();
    private final Map<String, FunctionMetadata> metaByName = new LinkedHashMap<String, FunctionMetadata>();

    public BuiltinFunctionCatalog() {
        // --- klassische Builtins ---
        register(new DateFunction());
        register(new EchoFunction());

        // --- Tool-Funktionen aus der ToolsRegistry ---
        ToolBuiltins toolBuiltins = new ToolBuiltins();
        for (ExpressionFunction f : toolBuiltins.all()) {
            register(f);
        }
    }

    public Set<String> names() { return Collections.unmodifiableSet(byName.keySet()); }

    public boolean contains(String name) {
        return name != null && byName.containsKey(normalize(name));
    }

    public ExpressionFunction get(String name) {
        return name == null ? null : byName.get(normalize(name));
    }

    public Collection<FunctionMetadata> metadata() {
        return Collections.unmodifiableCollection(metaByName.values());
    }

    private void register(ExpressionFunction fn) {
        String key = normalize(fn.metadata().getName());
        byName.put(key, fn);
        metaByName.put(key, fn.metadata());
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
