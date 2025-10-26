package de.bund.zrb.ui.expressions;

import de.bund.zrb.expressions.domain.ResolvableExpression;

/**
 * Listen for node selection events from ExpressionTreeComboBox.
 *
 * Intent:
 * - Notify higher layers ("User chose this AST node for field X").
 * - Avoid direct coupling between Swing component and scenario state.
 */
public interface ExpressionSelectionListener {

    /**
     * React to a completed selection from the popup tree.
     *
     * @param chosenLabel human readable label from the tree
     * @param chosenExpression the exact ResolvableExpression node that was selected
     */
    void onExpressionSelected(String chosenLabel,
                              ResolvableExpression chosenExpression);
}
