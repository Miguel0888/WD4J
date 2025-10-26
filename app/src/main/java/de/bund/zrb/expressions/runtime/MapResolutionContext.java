package de.bund.zrb.expressions.runtime;

import de.bund.zrb.expressions.domain.UnresolvedSymbolException;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Provide variable values from a Map<String,Object>.
 *
 * Intent:
 * - Act as adapter from your Cucumber scenario/world state to ResolutionContext.
 * - Support lazy suppliers for time-sensitive data.
 *
 * Behavior:
 * - If the stored value is a Supplier, call get() on resolve.
 * - Otherwise return the value directly.
 */
public class MapResolutionContext implements ResolutionContext {

    private final Map<String, Object> values;

    public MapResolutionContext(Map<String, Object> values) {
        this.values = values;
    }

    public boolean isAvailable(String name) {
        return values.containsKey(name);
    }

    public Object resolveVariable(String name) throws UnresolvedSymbolException {
        if (!values.containsKey(name)) {
            throw new UnresolvedSymbolException(name);
        }

        Object raw = values.get(name);

        // Support lazy values (Supplier), e.g. one-time tokens or session data.
        if (raw instanceof Supplier) {
            Supplier supplier = (Supplier) raw;
            return supplier.get();
        }

        return raw;
    }
}
