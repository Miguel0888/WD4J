package de.bund.zrb.expressions.spi;

import de.bund.zrb.expressions.domain.ExpressionFunction;
import de.bund.zrb.expressions.domain.FunctionMetadata;

import java.util.*;

public final class CompositeFunctionProvider implements FunctionProvider {

    private final List<FunctionProvider> providers = new ArrayList<FunctionProvider>();

    public CompositeFunctionProvider(List<FunctionProvider> providers) {
        if (providers != null) this.providers.addAll(providers);
    }

    public void add(FunctionProvider provider) {
        if (provider != null) providers.add(provider);
    }

    public Collection<ExpressionFunction> functions() {
        Map<String, ExpressionFunction> all = new LinkedHashMap<String, ExpressionFunction>();
        for (int i = 0; i < providers.size(); i++) {
            FunctionProvider p = providers.get(i);
            Collection<ExpressionFunction> fs = p.functions();
            for (ExpressionFunction f : fs) {
                all.put(f.metadata().getName().toLowerCase(), f);
            }
        }
        return all.values();
    }

    public Collection<FunctionMetadata> metadata() {
        Map<String, FunctionMetadata> all = new LinkedHashMap<String, FunctionMetadata>();
        for (int i = 0; i < providers.size(); i++) {
            FunctionProvider p = providers.get(i);
            Collection<FunctionMetadata> ms = p.metadata();
            for (FunctionMetadata m : ms) {
                all.put(m.getName().toLowerCase(), m);
            }
        }
        return all.values();
    }
}
