package de.bund.zrb.runtime;

import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.engine.CompositeExpression;
import de.bund.zrb.expressions.engine.FunctionExpression;
import de.bund.zrb.expressions.engine.LiteralExpression;
import de.bund.zrb.expressions.engine.VariableExpression;

import java.util.List;

/**
 * Turn an expression node back into the user-facing template syntax.
 *
 * Intent:
 * - When user picks a node in the UI tree, convert that node into a self-contained template string.
 * - Store that string directly in TestAction.setValue(...).
 *
 * Rules:
 * - VariableExpression("username") -> "{{username}}"
 * - FunctionExpression("OTP", [Variable("username")]) -> "{{OTP({{username}})}}"
 * - CompositeExpression([...]) -> concat parts
 * - LiteralExpression("foo") -> "foo"
 *
 * No side effects, no registry calls. Just stringify intent.
 */
public final class ExpressionTemplateRenderer {

    private ExpressionTemplateRenderer() {
        // utility
    }

    public static String render(ResolvableExpression expr) {
        if (expr instanceof VariableExpression) {
            VariableExpression v = (VariableExpression) expr;
            // {{username}}
            return "{{" + v.getName() + "}}";
        }

        if (expr instanceof LiteralExpression) {
            LiteralExpression lit = (LiteralExpression) expr;
            return lit.getText() != null ? lit.getText() : "";
        }

        if (expr instanceof FunctionExpression) {
            FunctionExpression fn = (FunctionExpression) expr;
            // {{FN(arg1,arg2,...)}}
            // aber Achtung: deine Syntax erlaubt verschachtelte {{...}} in Args.
            // Wir bilden daher die Args rekursiv mit render(...)
            StringBuilder sb = new StringBuilder();
            sb.append("{{");
            sb.append(fn.getFunctionName());
            sb.append("(");

            List<ResolvableExpression> args = fn.getArguments();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(render(args.get(i)));
            }

            sb.append(")");
            sb.append("}}");
            return sb.toString();
        }

        if (expr instanceof CompositeExpression) {
            CompositeExpression cmp = (CompositeExpression) expr;
            StringBuilder sb = new StringBuilder();
            List<ResolvableExpression> parts = cmp.getParts();
            for (int i = 0; i < parts.size(); i++) {
                sb.append(render(parts.get(i)));
            }
            return sb.toString();
        }

        // Fallback
        return "";
    }
}
