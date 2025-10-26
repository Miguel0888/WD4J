package de.bund.zrb.expressions.engine;

import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.domain.UnresolvedSymbolException;
import de.bund.zrb.expressions.runtime.ResolutionContext;

/**
 * Represent fixed literal text.
 *
 * Use this for:
 * - Plain text outside expressions
 * - Quoted string arguments in function calls ('foo')
 */
public class LiteralExpression implements ResolvableExpression {

    private final String literal;

    public LiteralExpression(String literal) {
        this.literal = literal != null ? literal : "";
    }

    public Object resolve(ResolutionContext context) throws UnresolvedSymbolException {
        return literal;
    }

    public boolean isResolved(ResolutionContext context) {
        return true;
    }

    public String toString() {
        return literal;
    }
}
