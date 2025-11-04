package de.bund.zrb.runtime;

import de.bund.zrb.expressions.builtins.EchoFunction;
import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionMetadata;
import de.bund.zrb.expressions.builtins.DateFunction;

import java.util.*;

public final class BuiltinFunctionCatalog {

    private final Map<String, ExpressionFunction> byName = new LinkedHashMap<String, ExpressionFunction>();
    private final Map<String, FunctionMetadata> metaByName = new LinkedHashMap<String, FunctionMetadata>();

    public BuiltinFunctionCatalog() {
        // Register all built-ins here
        register(new DateFunction());
        register(new EchoFunction());
        // register(new ToUpperFunction());
        // ...
    }

    // ----- API -----

    public Set<String> names() {
        return Collections.unmodifiableSet(byName.keySet());
    }

    public boolean contains(String name) {
        return name != null && byName.containsKey(name);
    }

    public ExpressionFunction get(String name) {
        return name == null ? null : byName.get(name);
    }

    public Collection<FunctionMetadata> metadata() {
        return Collections.unmodifiableCollection(metaByName.values());
    }

    // ----- intern -----

    private void register(ExpressionFunction fn) {
        String key = normalize(fn.metadata().getName());
        byName.put(key, fn);
        metaByName.put(key, fn.metadata());
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
