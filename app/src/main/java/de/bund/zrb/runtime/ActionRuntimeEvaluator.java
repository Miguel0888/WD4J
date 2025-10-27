package de.bund.zrb.runtime;

import de.bund.zrb.expressions.engine.ExpressionParser;
import de.bund.zrb.expressions.domain.ResolvableExpression;

/**
 * Turn a TestAction's saved template string (action.getValue())
 * into the final runtime string, using the provided scope.
 *
 * This is LAZY: call it right before executing the step.
 */
public final class ActionRuntimeEvaluator {

    private ActionRuntimeEvaluator() { }

    /**
     * Evaluate the template from the action using current runtime scope.
     * Return "" if anything fails.
     */
    public static String evaluateActionValue(String template,
                                             ValueScope scope) {
        if (template == null || template.trim().length() == 0) {
            return "";
        }
        try {
            // 1. Parse the template string (which is in {{...}} syntax)
            ExpressionParser parser = new ExpressionParser();
            ResolvableExpression ast = parser.parseTemplate(template);

            // 2. Evaluate entire AST recursively with current scope
            return RuntimeExpressionEvaluator.evalNode(ast, scope);

        } catch (Exception ex) {
            return "";
        }
    }
}
