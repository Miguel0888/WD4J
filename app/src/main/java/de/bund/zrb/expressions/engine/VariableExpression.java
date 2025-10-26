package de.bund.zrb.expressions.engine;

import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.domain.UnresolvedSymbolException;
import de.bund.zrb.expressions.runtime.ResolutionContext;

/**
 * Represent a variable reference like {{userName}}.
 *
 * Intent:
 * - Only describe "which variable".
 * - Ask ResolutionContext for the value.
 */
public class VariableExpression implements ResolvableExpression {

    private final String name;

    public VariableExpression(String name) {
        this.name = name;
    }

    public Object resolve(ResolutionContext context) throws UnresolvedSymbolException {
        return context.resolveVariable(name);
    }

    public boolean isResolved(ResolutionContext context) {
        return context.isAvailable(name);
    }

    public String toString() {
        return "{{" + name + "}}";
    }

    public String getName() {
        return name;
    }
}
