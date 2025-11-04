// src/main/java/de/bund/zrb/runtime/BuiltinFunctionCatalog.java
package de.bund.zrb.runtime;

import de.bund.zrb.expressions.builtins.EchoFunction;
import de.bund.zrb.expressions.builtins.DateFunction;
import de.bund.zrb.expressions.builtins.tooling.ToolFunctionsCollector;
import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionMetadata;
import de.bund.zrb.service.ToolsRegistry;

import java.util.*;

/**
 * Sammle alle fest verdrahteten (kompilierten) Built-in-Funktionen und stelle
 * sie dem System zur Verfügung. Tools, die Builtins anbieten, werden hier
 * ebenfalls integriert.
 */
public final class BuiltinFunctionCatalog {

    private final Map<String, ExpressionFunction> byName = new LinkedHashMap<String, ExpressionFunction>();
    private final Map<String, FunctionMetadata> metaByName = new LinkedHashMap<String, FunctionMetadata>();

    public BuiltinFunctionCatalog() {
        // 1) Klassische Builtins
        register(new DateFunction());
        register(new EchoFunction());

        // 2) Tool-Builtins dynamisch einsammeln (nur Tools, die BuiltinTool implementieren)
        Collection<ExpressionFunction> toolFns =
                ToolFunctionsCollector.collectFrom(ToolsRegistry.getInstance());
        for (ExpressionFunction f : toolFns) {
            register(f);
        }
    }

    // ----- API -----

    public Set<String> names() {
        return Collections.unmodifiableSet(byName.keySet());
    }

    public boolean contains(String name) {
        return name != null && byName.containsKey(normalize(name));
    }

    public ExpressionFunction get(String name) {
        return name == null ? null : byName.get(normalize(name));
    }

    public Collection<FunctionMetadata> metadata() {
        return Collections.unmodifiableCollection(metaByName.values());
    }

    /** Erlaube externe Registrierung (Tests/Plugins). */
    public void add(ExpressionFunction fn) {
        if (fn != null) register(fn);
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
