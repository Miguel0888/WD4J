// src/main/java/de/bund/zrb/runtime/BuiltinFunctionCatalog.java
package de.bund.zrb.runtime;

import de.bund.zrb.expressions.builtins.EchoFunction;
import de.bund.zrb.expressions.builtins.DateFunction;
import de.bund.zrb.expressions.builtins.tooling.BuiltinTool;
import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionMetadata;
import de.bund.zrb.service.ToolsRegistry;

import java.util.*;

/**
 * Collect all built-in functions from core and tools in one place.
 * Keep insertion order stable for deterministic UI.
 */
public final class BuiltinFunctionCatalog {

    private final Map<String, ExpressionFunction> byName = new LinkedHashMap<String, ExpressionFunction>();
    private final Map<String, FunctionMetadata> metaByName = new LinkedHashMap<String, FunctionMetadata>();

    public BuiltinFunctionCatalog() {
        registerCoreBuiltins();
        registerToolBuiltins();
    }

    // -------- public API -------------------------------------------------------

    public Set<String> names() {
        return Collections.unmodifiableSet(byName.keySet());
    }

    public boolean contains(String name) {
        return name != null && byName.containsKey(normalize(name));
    }

    public ExpressionFunction get(String name) {
        if (name == null) return null;
        return byName.get(normalize(name));
    }

    public Collection<ExpressionFunction> all() {
        return Collections.unmodifiableCollection(byName.values());
    }

    public Collection<FunctionMetadata> metadata() {
        return Collections.unmodifiableCollection(metaByName.values());
    }

    // -------- internal ---------------------------------------------------------

    private void registerCoreBuiltins() {
        // Add all core built-ins here
        register(new DateFunction());
        register(new EchoFunction());
        // register(new ToUpperFunction()); // example
    }

    private void registerToolBuiltins() {
        ToolsRegistry tr = ToolsRegistry.getInstance();

        // Collect every tool that implements BuiltinTool
        List<Object> tools = new ArrayList<Object>();
        tools.add(tr.navigationTool());
        tools.add(tr.screenshotTool());
        tools.add(tr.loginTool());
        tools.add(tr.twoFaTool());
        tools.add(tr.notificationTool());

        for (int i = 0; i < tools.size(); i++) {
            Object t = tools.get(i);
            if (t instanceof BuiltinTool) {
                BuiltinTool bt = (BuiltinTool) t;
                Collection<ExpressionFunction> fns = bt.builtinFunctions();
                if (fns != null) {
                    for (ExpressionFunction fn : fns) register(fn);
                }
            }
        }
    }

    private void register(ExpressionFunction fn) {
        if (fn == null || fn.metadata() == null) return;
        String key = normalize(fn.metadata().getName());
        byName.put(key, fn);
        metaByName.put(key, fn.metadata());
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }
}
