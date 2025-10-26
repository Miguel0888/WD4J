package de.bund.zrb.expressions.engine;

import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.domain.UnresolvedSymbolException;
import de.bund.zrb.expressions.runtime.ResolutionContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent a concatenation of multiple subexpressions, in order.
 *
 * Example:
 *   "Hallo " + {{userName}} + ", Code: " + {{otp()}}
 *
 * Resolve logic:
 * - Resolve each part, append .toString().
 */
public class CompositeExpression implements ResolvableExpression {

    private final List<ResolvableExpression> parts;

    public CompositeExpression(List<ResolvableExpression> parts) {
        this.parts = new ArrayList<ResolvableExpression>(parts);
    }

    public Object resolve(ResolutionContext context) throws UnresolvedSymbolException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            Object value = parts.get(i).resolve(context);
            if (value != null) {
                sb.append(value.toString());
            }
        }
        return sb.toString();
    }

    public boolean isResolved(ResolutionContext context) {
        for (int i = 0; i < parts.size(); i++) {
            if (!parts.get(i).isResolved(context)) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "CompositeExpression(" + parts.size() + " parts)";
    }

    public List<ResolvableExpression> getParts() {
        return parts;
    }
}
