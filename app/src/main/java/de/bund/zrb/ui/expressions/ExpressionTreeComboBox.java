package de.bund.zrb.ui.expressions;

import de.bund.zrb.expressions.domain.ResolvableExpression;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Show a dropdown-like control that opens a popup containing a JTree of expressions.
 *
 * Intent:
 * - Allow the test author to pick exactly one AST node (ResolvableExpression)
 *   from the expression tree created in the Given-step.
 * - Store that choice so it can be used later in a When-step.
 *
 * SRP:
 * - This component only deals with UI presentation and user interaction.
 * - It does NOT resolve expressions or talk to ScenarioState directly.
 *
 * Usage:
 *   ExpressionTreeComboBox combo = new ExpressionTreeComboBox();
 *   combo.setTreeRoot(myUiRoot); // built from ExpressionTreeNode.fromExpression(...)
 *   combo.addSelectionListener(...); // react to user choice
 *
 * Notes:
 * - Java 8 compatible, no lambdas required from the outside.
 */
public class ExpressionTreeComboBox extends JPanel {

    private final JTextField displayField;
    private final JButton dropButton;
    private final JPopupMenu popup;
    private final JTree tree;

    private final List<ExpressionSelectionListener> listeners =
            new ArrayList<ExpressionSelectionListener>();

    public ExpressionTreeComboBox() {
        super(new BorderLayout());

        // Non-editable text to display current selection
        displayField = new JTextField();
        displayField.setEditable(false);

        // Small button to open/close popup
        dropButton = new JButton("▼");
        dropButton.setMargin(new Insets(0, 4, 0, 4));
        dropButton.setFocusable(false);

        // Tree inside a scroll pane inside a popup
        tree = new JTree(new DefaultMutableTreeNode("No data"));
        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(createRenderer());

        JScrollPane scroll = new JScrollPane(tree);
        scroll.setPreferredSize(new Dimension(300, 200));

        popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.add(scroll, BorderLayout.CENTER);

        add(displayField, BorderLayout.CENTER);
        add(dropButton, BorderLayout.EAST);

        wireInteractions();
    }

    /**
     * Inject a new logical root for the tree.
     * Build a Swing TreeModel from the provided ExpressionTreeNode.
     */
    public void setTreeRoot(ExpressionTreeNode uiRoot) {
        if (uiRoot == null) {
            tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode("No data")));
            return;
        }

        ExpressionTreeModelBuilder builder = new ExpressionTreeModelBuilder();
        DefaultMutableTreeNode swingRoot = builder.buildSwingTree(uiRoot);
        DefaultTreeModel model = new DefaultTreeModel(swingRoot);

        tree.setModel(model);
        expandAll();
    }

    /**
     * Return the currently chosen expression node (or null if none).
     * This reads the text field's client property where we stash it.
     */
    public ResolvableExpression getSelectedExpression() {
        Object obj = displayField.getClientProperty("chosenExpr");
        if (obj instanceof ResolvableExpression) {
            return (ResolvableExpression) obj;
        }
        return null;
    }

    /**
     * Return the label of the currently chosen node (or empty string).
     */
    public String getSelectedLabel() {
        Object obj = displayField.getClientProperty("chosenLabel");
        if (obj instanceof String) {
            return (String) obj;
        }
        return "";
    }

    /**
     * Register external listener to be notified when the user selects a node.
     */
    public void addSelectionListener(ExpressionSelectionListener l) {
        if (l != null) {
            listeners.add(l);
        }
    }

    private void fireSelection(ResolvableExpression expr, String label) {
        // Update display
        displayField.setText(label);
        displayField.putClientProperty("chosenExpr", expr);
        displayField.putClientProperty("chosenLabel", label);

        // Inform listeners
        for (int i = 0; i < listeners.size(); i++) {
            ExpressionSelectionListener l = listeners.get(i);
            l.onExpressionSelected(label, expr);
        }
    }

    private void wireInteractions() {
        // Open popup on button
        dropButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showPopup();
            }
        });

        // Handle keyboard "Enter" to confirm selection
        tree.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (KeyEvent.VK_ENTER == e.getKeyCode()) {
                    confirmCurrentTreeSelection();
                }
            }
        });

        // Handle mouse double-click to confirm selection
        tree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    confirmCurrentTreeSelection();
                }
            }
        });

        // Optional: show currently hovered selection in text field or tooltip.
        // We keep it simple here: we only set selection on confirm.
    }

    private void showPopup() {
        // Position popup directly under this component
        popup.show(this, 0, this.getHeight());
        tree.requestFocusInWindow();
    }

    private void hidePopup() {
        popup.setVisible(false);
    }

    /**
     * Confirm the currently selected node in the tree
     * and fire ExpressionSelectionListener.
     */
    private void confirmCurrentTreeSelection() {
        TreePath selPath = tree.getSelectionPath();
        if (selPath == null) {
            hidePopup();
            return;
        }

        Object last = selPath.getLastPathComponent();
        if (!(last instanceof DefaultMutableTreeNode)) {
            hidePopup();
            return;
        }

        DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) last;
        Object userObj = dmtn.getUserObject();

        if (userObj instanceof ExpressionTreeModelBuilder.NodePayload) {
            ExpressionTreeModelBuilder.NodePayload payload =
                    (ExpressionTreeModelBuilder.NodePayload) userObj;

            ResolvableExpression expr = payload.getExpression();
            String label = payload.getLabel();

            fireSelection(expr, label);
        }

        hidePopup();
    }

    /**
     * Expand all nodes for usability.
     * This makes it feel more like a "tree dropdown" instead of a collapsed navigator.
     */
    private void expandAll() {
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    /**
     * Create a cell renderer that uses the NodePayload label.
     * Keep it readable and slightly indented by JTree itself.
     */
    private TreeCellRenderer createRenderer() {
        return new DefaultTreeCellRenderer() {
            public Component getTreeCellRendererComponent(JTree tree,
                                                          Object value,
                                                          boolean sel,
                                                          boolean expanded,
                                                          boolean leaf,
                                                          int row,
                                                          boolean hasFocus) {
                Component c = super.getTreeCellRendererComponent(
                        tree, value, sel, expanded, leaf, row, hasFocus);

                if (value instanceof DefaultMutableTreeNode) {
                    DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) value;
                    Object userObj = dmtn.getUserObject();
                    if (userObj instanceof ExpressionTreeModelBuilder.NodePayload) {
                        ExpressionTreeModelBuilder.NodePayload payload =
                                (ExpressionTreeModelBuilder.NodePayload) userObj;
                        setText(payload.getLabel());
                        // keep default icon styling (variable vs function icons
                        // könnte man später ergänzen)
                    }
                }

                return c;
            }
        };
    }
}
