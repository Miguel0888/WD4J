package de.bund.zrb.expressions.runtime;

import de.bund.zrb.expressions.domain.UnresolvedSymbolException;

/**
 * Provide variable values at runtime.
 *
 * Intent:
 * - Decouple expressions from a concrete data source (Map, Scenario, DB, etc.).
 * - Allow lazy values (e.g. Supplier) without changing the parser.
 */
public interface ResolutionContext {

    /**
     * Return true if the given variable name is available.
     */
    boolean isAvailable(String name);

    /**
     * Return the resolved value for the given variable as Object.
     * Throw if not available.
     */
    Object resolveVariable(String name) throws UnresolvedSymbolException;
}
