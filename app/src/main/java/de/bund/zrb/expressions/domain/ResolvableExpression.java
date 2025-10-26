package de.bund.zrb.expressions.domain;

import de.bund.zrb.expressions.runtime.ResolutionContext;

/**
 * Describe something that can be resolved to a concrete runtime value.
 *
 * Intent:
 * - Model each placeholder or literal as an object.
 * - Force callers to explicitly resolve values with a given ResolutionContext.
 */
public interface ResolvableExpression {

    /**
     * Resolve this expression to a concrete value using the provided context.
     * Throw UnresolvedSymbolException if something is missing.
     */
    Object resolve(ResolutionContext context) throws UnresolvedSymbolException;

    /**
     * Return true if this expression can be fully resolved with the provided context.
     */
    boolean isResolved(ResolutionContext context);
}
