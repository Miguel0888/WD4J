package de.bund.zrb.runtime;

import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.engine.CompositeExpression;
import de.bund.zrb.expressions.engine.FunctionExpression;
import de.bund.zrb.expressions.engine.LiteralExpression;
import de.bund.zrb.expressions.engine.VariableExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluate a ResolvableExpression node against a ValueScope.
 *
 * Intent:
 *  - Use this right before executing a TestAction.
 *  - Keep it lazy: never cache OTP etc. before runtime.
 *
 * Rules:
 *  VariableExpression("username"):
 *      -> look up username in scope chain (test case -> suite -> root)
 *
 *  FunctionExpression("OTP", [ VariableExpression("username") ]):
 *      -> evaluate args recursively
 *      -> call ExpressionRegistry through scope.callFunction("OTP", argsAsStrings)
 *
 *  CompositeExpression([...]):
 *      -> evaluate each child and append
 *
 *  LiteralExpression("abc"):
 *      -> return "abc"
 */
public final class RuntimeExpressionEvaluator {

    private RuntimeExpressionEvaluator() {
        // utility
    }

    /**
     * Evaluate the given node into a String.
     * Return "" if nothing resolves.
     */
    public static String evalNode(ResolvableExpression expr, ValueScope scope) {

        if (expr instanceof VariableExpression) {
            VariableExpression var = (VariableExpression) expr;
            String value = scope.resolveVariable(var.getName());
            return (value != null) ? value : "";
        }

        if (expr instanceof LiteralExpression) {
            LiteralExpression lit = (LiteralExpression) expr;
            return (lit.getText() != null) ? lit.getText() : "";
        }

        if (expr instanceof FunctionExpression) {
            FunctionExpression fun = (FunctionExpression) expr;

            // Step 1: evaluate arguments recursively
            List<String> evaluatedArgs = new ArrayList<String>();
            List<ResolvableExpression> args = fun.getArguments();
            for (int i = 0; i < args.size(); i++) {
                ResolvableExpression argExpr = args.get(i);
                String argVal = evalNode(argExpr, scope);
                evaluatedArgs.add(argVal);
            }

            // Step 2: call function via ExpressionRegistry
            String result = scope.callFunction(fun.getFunctionName(), evaluatedArgs);
            return (result != null) ? result : "";
        }

        if (expr instanceof CompositeExpression) {
            CompositeExpression cmp = (CompositeExpression) expr;
            StringBuilder sb = new StringBuilder();
            List<ResolvableExpression> parts = cmp.getParts();
            for (int i = 0; i < parts.size(); i++) {
                sb.append(evalNode(parts.get(i), scope));
            }
            return sb.toString();
        }

        // Unknown expression type. Fail soft.
        return "";
    }
}
