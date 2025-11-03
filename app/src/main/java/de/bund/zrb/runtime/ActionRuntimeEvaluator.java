package de.bund.zrb.runtime;

import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.engine.CompositeExpression;
import de.bund.zrb.expressions.engine.ExpressionParser;
import de.bund.zrb.expressions.engine.FunctionExpression;
import de.bund.zrb.expressions.engine.LiteralExpression;
import de.bund.zrb.expressions.engine.VariableExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * Wandelt action.getValue() (z.B. "{{username}}", "{{otp()}}", "{{wrap({{username}},\"X\")}}")
 * zur Laufzeit in einen konkreten String um.
 *
 * Spielregeln:
 * - Wenn KEINE {{...}}-Syntax drin steckt -> gib den String 1:1 zurück.
 * - Sonst:
 *   - Parse mit ExpressionParser
 *   - Variablen -> ValueScope.lookupVar(name)
 *   - Funktionsaufrufe -> scope.getExpressionRegistry().evaluate(fnName, argStrings...)
 *   - Composite -> Teile zusammenkleben
 */
public final class ActionRuntimeEvaluator {

    private ActionRuntimeEvaluator() {}

    public static String evaluateActionValue(String template, ValueScope scope) {
        if (template == null || template.trim().isEmpty()) {
            return "";
        }

        // Fast path: keine "{{"
        if (!template.contains("{{")) {
            return template;
        }

        // 1. parse in AST
        ExpressionParser parser = new ExpressionParser();
        ResolvableExpression ast = parser.parseTemplate(template);

        // 2. rekursiv auswerten
        return eval(ast, scope);
    }

    private static String eval(ResolvableExpression expr, ValueScope scope) {
        if (expr == null) return "";

        // LiteralExpression = normaler Textteil
        if (expr instanceof LiteralExpression) {
            return ((LiteralExpression) expr).getText();
        }

        // VariableExpression = {{username}}
        if (expr instanceof VariableExpression) {
            String name = ((VariableExpression) expr).getName();
            if (name == null) return "";
            // Variablen-Namen kommen OHNE Präfix "{{" "}}", OHNE "*"
            // Shadowing in scope.lookupVar(...)
            String val = scope.lookupVar(name);
            if (val != null) {
                return val;
            }

            // Falls diese "Variable" eigentlich ein Template-Name ist:
            String tmpl = scope.lookupTemplate(name);
            if (tmpl != null && tmpl.contains("{{")) {
                // rekursive Auswertung des Template-Strings
                return evaluateActionValue(tmpl, scope);
            } else if (tmpl != null) {
                return tmpl;
            }

            return "";
        }

        // FunctionExpression = {{otp()}}, {{wrap({{username}},"X")}}
        if (expr instanceof FunctionExpression) {
            FunctionExpression fn = (FunctionExpression) expr;
            String fnName = fn.getFunctionName();

            // Argumente rekursiv auswerten (sind selber ResolvableExpression)
            List<String> argVals = new ArrayList<String>();
            for (ResolvableExpression argExpr : fn.getArgs()) {
                argVals.add(eval(argExpr, scope));
            }

            // An Registry delegieren
            ExpressionRegistry reg = scope.getExpressionRegistry();
            return reg.evaluate(fnName, argVals);
        }

        // CompositeExpression = Verkettung mehrerer Teile
        if (expr instanceof CompositeExpression) {
            CompositeExpression cx = (CompositeExpression) expr;
            StringBuilder sb = new StringBuilder();
            for (ResolvableExpression part : cx.getParts()) {
                sb.append(eval(part, scope));
            }
            return sb.toString();
        }

        // Unbekannter Knoten? → leer
        return "";
    }
}
