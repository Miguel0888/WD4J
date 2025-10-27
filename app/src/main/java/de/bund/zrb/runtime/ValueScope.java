package de.bund.zrb.runtime;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hold runtime data for expression evaluation.
 *
 * Intent:
 *  - Resolve variables like {{username}} to concrete values.
 *  - Call functions like {{OTP(...)}} through ExpressionRegistry.
 *  - Support hierarchical lookup: caseScope -> suiteScope -> rootScope.
 *
 * Usage pattern:
 *  ValueScope root = new ValueScope(exprRegistry, null);
 *  root.putVariable("env", "INT");
 *
 *  ValueScope suite = new ValueScope(exprRegistry, root);
 *  suite.putVariable("mandant", "4711");
 *
 *  ValueScope tc = new ValueScope(exprRegistry, suite);
 *  tc.putVariable("username", "alice");
 *
 *  // later: evaluator.resolveVariable("username") returns "alice"
 *  // fallback walks up to parents if missing here.
 */
public class ValueScope {

    private final Map<String, String> variables = new LinkedHashMap<String, String>();
    private final ExpressionRegistry expressionRegistry;
    private final ValueScope parent;

    public ValueScope(ExpressionRegistry registry, ValueScope parent) {
        this.expressionRegistry = registry;
        this.parent = parent;
    }

    /**
     * Register / override a variable in this scope.
     */
    public void putVariable(String name, String value) {
        if (name != null && value != null) {
            variables.put(name, value);
        }
    }

    /**
     * Resolve variable by walking this scope upwards.
     * Return null if not found anywhere.
     */
    public String resolveVariable(String name) {
        if (name == null) {
            return null;
        }
        if (variables.containsKey(name)) {
            return variables.get(name);
        }
        if (parent != null) {
            return parent.resolveVariable(name);
        }
        return null;
    }

    /**
     * Call a function like OTP(...) via ExpressionRegistry.
     * All args are already evaluated to plain strings.
     *
     * Return null if call fails.
     */
    public String callFunction(String functionName, List<String> evaluatedArgs) {
        if (expressionRegistry == null) {
            return null;
        }
        try {
            // ExpressionRegistry.evaluate(String key, List<String> params)
            return expressionRegistry.evaluate(functionName, evaluatedArgs);
        } catch (Exception ex) {
            // Fail soft. Return null on error.
            return null;
        }
    }
}
