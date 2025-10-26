package de.bund.zrb.ui.expressions;

import de.bund.zrb.expressions.domain.ResolvableExpression;
import de.bund.zrb.expressions.engine.CompositeExpression;
import de.bund.zrb.expressions.engine.FunctionExpression;
import de.bund.zrb.expressions.engine.LiteralExpression;
import de.bund.zrb.expressions.engine.VariableExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represent a UI-facing wrapper for a ResolvableExpression AST node.
 *
 * Intent:
 * - Provide display label for Swing UI.
 * - Provide child nodes for tree navigation.
 * - Hold direct reference to the underlying ResolvableExpression.
 *
 * SRP:
 * - This class only maps expression structure to a UI-friendly node graph.
 *   It does not know anything about Swing components or popups.
 */
public class ExpressionTreeNode {

    private final ResolvableExpression expression;
    private final String displayLabel;
    private final List<ExpressionTreeNode> children;

    public ExpressionTreeNode(ResolvableExpression expression,
                              String displayLabel,
                              List<ExpressionTreeNode> children) {
        this.expression = expression;
        this.displayLabel = displayLabel;
        this.children = new ArrayList<ExpressionTreeNode>(children);
    }

    public ResolvableExpression getExpression() {
        return expression;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public List<ExpressionTreeNode> getChildren() {
        return new ArrayList<ExpressionTreeNode>(children);
    }

    /**
     * Build ExpressionTreeNode recursively from a ResolvableExpression.
     * Add light semantic labels to help test authors understand the node.
     */
    public static ExpressionTreeNode fromExpression(ResolvableExpression expr) {

        if (expr instanceof VariableExpression) {
            VariableExpression v = (VariableExpression) expr;
            String label = "Variable: " + v.getName();
            return new ExpressionTreeNode(expr, label,
                    Collections.<ExpressionTreeNode>emptyList());
        }

        if (expr instanceof FunctionExpression) {
            FunctionExpression f = (FunctionExpression) expr;
            String label = "Function: " + f.getFunctionName();

            List<ExpressionTreeNode> argNodes = new ArrayList<ExpressionTreeNode>();
            List<ResolvableExpression> args = f.getArguments();
            for (int i = 0; i < args.size(); i++) {
                ResolvableExpression argExpr = args.get(i);
                argNodes.add(fromExpression(argExpr));
            }

            return new ExpressionTreeNode(expr, label, argNodes);
        }

        if (expr instanceof CompositeExpression) {
            CompositeExpression c = (CompositeExpression) expr;
            String label = "Composite";

            List<ExpressionTreeNode> partNodes = new ArrayList<ExpressionTreeNode>();
            List<ResolvableExpression> parts = c.getParts();
            for (int i = 0; i < parts.size(); i++) {
                partNodes.add(fromExpression(parts.get(i)));
            }

            return new ExpressionTreeNode(expr, label, partNodes);
        }

        if (expr instanceof LiteralExpression) {
            LiteralExpression lit = (LiteralExpression) expr;
            // Use toString() of LiteralExpression which returns the literal text
            String label = "Literal: \"" + lit.toString() + "\"";
            return new ExpressionTreeNode(expr, label,
                    Collections.<ExpressionTreeNode>emptyList());
        }

        // Fallback for custom/unknown expression types
        return new ExpressionTreeNode(expr, "Expression", Collections.<ExpressionTreeNode>emptyList());
    }
}
