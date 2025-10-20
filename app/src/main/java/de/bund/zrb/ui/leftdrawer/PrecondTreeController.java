package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.model.Precondition;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.ui.TestNode;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

/**
 * Encapsulate all behavior for the "Preconditions" tree to keep LeftDrawer slim.
 * Methods are copied from LeftDrawer unchanged in content and comments (bugfix: missing brace).
 */
public class PrecondTreeController {

    private final JTree precondTree;

    public PrecondTreeController(JTree precondTree) {
        this.precondTree = precondTree;
    }

    // ========================= Preconditions tab =========================

    public static JTree buildPrecondTree() {
        TestNode root = new TestNode("Preconditions");
        return new JTree(root);
    }

    public void refreshPreconditions() {
        TestNode root = new TestNode("Preconditions");

        for (Precondition p : PreconditionRegistry.getInstance().getAll()) {
            // Show name; optionally include (id) to help debugging
            String display = (p.getName() != null && !p.getName().trim().isEmpty())
                    ? p.getName().trim()
                    : "(unnamed)";
            TestNode preNode = new TestNode(display, p);

            // List actions (When) as children
            if (p.getActions() != null) {
                for (TestAction action : p.getActions()) {
                    String label = renderActionLabel(action);
                    TestNode stepNode = new TestNode(label, action);
                    preNode.add(stepNode);
                }
            }

            root.add(preNode);
        }

        DefaultTreeModel model = (DefaultTreeModel) precondTree.getModel();
        model.setRoot(root);
        model.reload();
        // Expand root for better visibility
        precondTree.expandPath(new TreePath(root.getPath()));
    }

    public String renderActionLabel(TestAction action) {
        String label = action.getAction();
        if (action.getValue() != null && !action.getValue().isEmpty()) {
            label += " [" + action.getValue() + "]";
        } else if (action.getSelectedSelector() != null && !action.getSelectedSelector().isEmpty()) {
            label += " [" + action.getSelectedSelector() + "]";
        }
        return label;
    }
}
