package de.bund.zrb.expressions.builtins;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionMetadata;
import de.bund.zrb.expressions.spi.FunctionProvider;

import java.util.*;

public final class BuiltinFunctionProvider implements FunctionProvider {

    private final Map<String, ExpressionFunction> byName = new LinkedHashMap<String, ExpressionFunction>();
    private final List<FunctionMetadata> metadata = new ArrayList<FunctionMetadata>();

    public BuiltinFunctionProvider() {
        // Register built-in functions here
        register(new EchoFunction());
        register(new DateFunction());
        // Add more built-ins here...
    }

    private void register(ExpressionFunction fn) {
        String key = fn.metadata().getName().toLowerCase();
        byName.put(key, fn);
        metadata.add(fn.metadata());
    }

    public Collection<ExpressionFunction> functions() {
        return Collections.unmodifiableCollection(byName.values());
    }

    public Collection<FunctionMetadata> metadata() {
        return Collections.unmodifiableCollection(metadata);
    }

    public ExpressionFunction find(String name) {
        if (name == null) return null;
        return byName.get(name.toLowerCase());
    }
}
