package de.bund.zrb.expressions.engine;

import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.domain.UnresolvedSymbolException;
import de.bund.zrb.expressions.runtime.ResolutionContext;
import de.bund.zrb.runtime.ExpressionRegistry;
import de.bund.zrb.runtime.ExpressionRegistryImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent a function call like {{otp()}} or {{wrap('x';{{userName}})}}.
 *
 * Grammar:
 *   functionName '(' arg1 ; arg2 ; ... ')'
 *
 * Each argument is itself a ResolvableExpression (literal, variable, nested function).
 *
 * Resolve logic:
 * - Resolve all arguments first (to String).
 * - Call the ExpressionRegistry with (functionName, argsAsStrings).
 * - Return the registry result.
 *
 * Lazy:
 * - Do not precompute values. Only do it in resolve().
 *   This is where otp() becomes time-accurate.
 */
public class FunctionExpression implements ResolvableExpression {

    private final String functionName;
    private final List<ResolvableExpression> arguments;
    private final ExpressionRegistry registry;

    public FunctionExpression(String functionName,
                              List<ResolvableExpression> arguments) {
        this.functionName = functionName;
        this.arguments = new ArrayList<ResolvableExpression>(arguments);
        this.registry = ExpressionRegistryImpl.getInstance();
    }

    public Object resolve(ResolutionContext context) throws UnresolvedSymbolException {
        List<String> resolvedArgs = new ArrayList<String>();
        for (int i = 0; i < arguments.size(); i++) {
            Object argValue = arguments.get(i).resolve(context);
            resolvedArgs.add(argValue == null ? "" : argValue.toString());
        }

        // Ask the registry to evaluate the function at runtime (lazy).
        try {
            return registry.evaluate(functionName, resolvedArgs);
        } catch (Exception e) {
            throw new UnresolvedSymbolException(e.getMessage()); // ToDo: Check this
        }
    }

    public boolean isResolved(ResolutionContext context) {
        for (int i = 0; i < arguments.size(); i++) {
            if (!arguments.get(i).isResolved(context)) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "Function(" + functionName + ")";
    }

    public String getFunctionName() {
        return functionName;
    }

    public List<ResolvableExpression> getArguments() {
        return arguments;
    }

    public List<ResolvableExpression> getArgs() {
        return arguments;
    }
}
