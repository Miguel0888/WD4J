package de.bund.zrb.ui.expressions;

import de.bund.zrb.expressions.domain.ResolvableExpression;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Build a Swing tree model (DefaultMutableTreeNode) from an ExpressionTreeNode graph.
 *
 * Intent:
 * - Keep Swing-specific details out of ExpressionTreeNode.
 * - Attach the ResolvableExpression directly to each DefaultMutableTreeNode via userObject,
 *   so selection can return the chosen AST node for runtime evaluation.
 */
public class ExpressionTreeModelBuilder {

    /**
     * Convert the given ExpressionTreeNode into a Swing node recursively.
     * The Swing node's userObject will be a NodePayload object containing
     * both display label and the underlying ResolvableExpression.
     */
    public DefaultMutableTreeNode buildSwingTree(ExpressionTreeNode rootUiNode) {
        return buildNodeRecursive(rootUiNode);
    }

    private DefaultMutableTreeNode buildNodeRecursive(ExpressionTreeNode uiNode) {
        NodePayload payload = new NodePayload(
                uiNode.getDisplayLabel(),
                uiNode.getExpression()
        );

        DefaultMutableTreeNode swingNode = new DefaultMutableTreeNode(payload);

        for (int i = 0; i < uiNode.getChildren().size(); i++) {
            ExpressionTreeNode childUi = uiNode.getChildren().get(i);
            swingNode.add(buildNodeRecursive(childUi));
        }

        return swingNode;
    }

    /**
     * Small value object stored in each Swing tree node.
     *
     * Intent:
     * - Provide toString() for rendering in JTree.
     * - Keep direct access to the ResolvableExpression that this row represents.
     */
    public static class NodePayload {
        private final String label;
        private final ResolvableExpression expression;

        public NodePayload(String label, ResolvableExpression expression) {
            this.label = label;
            this.expression = expression;
        }

        public String getLabel() {
            return label;
        }

        public ResolvableExpression getExpression() {
            return expression;
        }

        public String toString() {
            // Let JTree cell renderer use this string
            return label;
        }
    }
}
