package de.bund.zrb.ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.*;
import java.io.IOException;

/**
 * Enables true MOVE drag & drop for Testsuite JTree.
 */
public class TestSuiteTreeTransferHandler extends TransferHandler {

    /**
     * A custom DataFlavor used to transfer tree nodes within the same JVM.  Using
     * the {@link DataFlavor#javaJVMLocalObjectMimeType} prevents the node (and
     * its userObject) from being serialized during a drag operation.  Without
     * this, the default {@code application/x-java-serialized-object} MIME type
     * would be used, which attempts to serialize the entire tree node.  Since
     * {@link de.bund.zrb.model.TestAction} and some of the other model classes
     * do not implement {@link java.io.Serializable}, the drag operation would
     * fail with a {@link java.io.NotSerializableException}.  By using the JVM
     * local MIME type we ensure that objects are passed by reference within
     * this JVM and no serialization occurs.
     */
    private final DataFlavor nodesFlavor;

    /**
     * Keeps track of the node currently being dragged.  This reference is
     * cleared after the drop is handled so that exportDone does not attempt
     * to remove the node a second time.
     */
    private DefaultMutableTreeNode draggedNode; // Save reference for exportDone

    public TestSuiteTreeTransferHandler() {
        try {
            // Define a DataFlavor that uses the local JVM object MIME type to avoid serialization
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=" + DefaultMutableTreeNode.class.getName();
            nodesFlavor = new DataFlavor(mimeType);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to create DataFlavor for JTree nodes", e);
        }
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        JTree tree = (JTree) c;
        TreePath path = tree.getSelectionPath();
        if (path == null) return null;

        draggedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        return new NodesTransferable(draggedNode);
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE; // MOVE only
    }

    @Override
    public boolean canImport(TransferSupport support) {
        if (!support.isDrop()) return false;
        support.setShowDropLocation(true);
        return support.isDataFlavorSupported(nodesFlavor);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support)) return false;

        DropContext ctx = buildDropContext(support);
        if (!ctx.isValid()) return false;

        try {
            DefaultMutableTreeNode moved = getMovedNode(support);
            if (moved == null || moved.getParent() == null) return false; // Root selbst nie verschieben
            if (!isDropAllowed(moved, ctx.dropTarget)) return false;

            MovePlan plan = computeMovePlan(moved, ctx.dropTarget, ctx.childIndex);
            if (plan == null) return false; // unerlaubte Kombination

            // Einfügeposition korrigieren, falls im selben Parent verschoben wird
            plan.insertIndex = adjustForSameParent((DefaultMutableTreeNode) moved.getParent(), moved, plan.insertIndex);

            // Domänenmodell (Listen) aktualisieren
            applyDomainMove(moved, plan.newParentNode, plan.insertIndex);

            // Baum aktualisieren & speichern
            applyTreeMove(ctx.tree, moved, plan.newParentNode, plan.insertIndex);
            de.bund.zrb.service.TestRegistry.getInstance().save();

            // Verhindern, dass exportDone nochmals entfernt
            draggedNode = null;
            return true;
        } catch (UnsupportedFlavorException | IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

    /* ========================== Hilfsmethoden ========================== */

    private static final class DropContext {
        final JTree tree;
        final JTree.DropLocation dl;
        final TreePath destPath;
        final DefaultMutableTreeNode dropTarget;
        final int childIndex;

        DropContext(JTree tree, JTree.DropLocation dl, TreePath destPath,
                    DefaultMutableTreeNode dropTarget, int childIndex) {
            this.tree = tree;
            this.dl = dl;
            this.destPath = destPath;
            this.dropTarget = dropTarget;
            this.childIndex = childIndex;
        }
        boolean isValid() { return tree != null && dl != null && destPath != null && dropTarget != null; }
    }

    private static final class MovePlan {
        DefaultMutableTreeNode newParentNode;
        int insertIndex;
        MovePlan(DefaultMutableTreeNode parent, int index) { this.newParentNode = parent; this.insertIndex = index; }
    }

    private DropContext buildDropContext(TransferSupport support) {
        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath destPath = dl != null ? dl.getPath() : null;
        JTree tree = (JTree) support.getComponent();
        DefaultMutableTreeNode dropTarget = destPath != null ? (DefaultMutableTreeNode) destPath.getLastPathComponent() : null;
        int childIndex = dl != null ? dl.getChildIndex() : -1;
        return new DropContext(tree, dl, destPath, dropTarget, childIndex);
    }

    private DefaultMutableTreeNode getMovedNode(TransferSupport support)
            throws UnsupportedFlavorException, IOException {
        return (DefaultMutableTreeNode) support.getTransferable().getTransferData(nodesFlavor);
    }

    private boolean isDropAllowed(DefaultMutableTreeNode moved, DefaultMutableTreeNode dropTarget) {
        // nicht in sich selbst / Nachfahren droppen
        if (moved.isNodeAncestor(dropTarget)) return false;
        return true;
    }

    private MovePlan computeMovePlan(DefaultMutableTreeNode moved,
                                     DefaultMutableTreeNode dropTarget,
                                     int childIndex) {
        // INSERT-Fall (Linie sichtbar): immer in den Parent (dropTarget) bei childIndex einfügen
        if (childIndex >= 0) {
            return planForInsert(moved, dropTarget, childIndex);
        }
        // ON-Fall (kein childIndex): „nach Ziel“ bzw. „ans Ende“ je nach Typen
        return planForOnDrop(moved, dropTarget);
    }

    private MovePlan planForInsert(DefaultMutableTreeNode moved,
                                   DefaultMutableTreeNode dropTarget,
                                   int childIndex) {
        Object movedObj = ((TestNode) moved).getModelRef();
        Object parentObj = dropTarget instanceof TestNode ? ((TestNode) dropTarget).getModelRef() : null;

        // Root (null) → nur Suites erlaubt
        if (parentObj == null) {
            if (movedObj instanceof de.bund.zrb.model.TestSuite) {
                return new MovePlan(dropTarget, childIndex);
            }
            return null;
        }
        // Suite → nur Cases
        if (parentObj instanceof de.bund.zrb.model.TestSuite) {
            if (movedObj instanceof de.bund.zrb.model.TestCase) {
                return new MovePlan(dropTarget, childIndex);
            }
            return null;
        }
        // Case → nur Actions (Steps)
        if (parentObj instanceof de.bund.zrb.model.TestCase) {
            if (movedObj instanceof de.bund.zrb.model.TestAction) {
                return new MovePlan(dropTarget, childIndex);
            }
            return null;
        }
        // Sonst (z. B. Action als Parent) nicht erlaubt
        return null;
    }

    private MovePlan planForOnDrop(DefaultMutableTreeNode moved,
                                   DefaultMutableTreeNode dropTarget) {
        Object movedObj = ((TestNode) moved).getModelRef();
        Object dropObj = dropTarget instanceof TestNode ? ((TestNode) dropTarget).getModelRef() : null;

        // Action wird gedroppt …
        if (movedObj instanceof de.bund.zrb.model.TestAction) {
            if (dropObj instanceof de.bund.zrb.model.TestCase) {
                // … auf Case → ans Ende des Cases
                return new MovePlan(dropTarget, dropTarget.getChildCount());
            } else if (dropObj instanceof de.bund.zrb.model.TestAction) {
                // … auf Action → nach dieser Action in deren Parent
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dropTarget.getParent();
                if (parent == null) return null;
                return new MovePlan(parent, parent.getIndex(dropTarget) + 1);
            }
            return null;
        }

        // Case wird gedroppt …
        if (movedObj instanceof de.bund.zrb.model.TestCase) {
            if (dropObj instanceof de.bund.zrb.model.TestSuite) {
                // … auf Suite → ans Ende der Suite
                return new MovePlan(dropTarget, dropTarget.getChildCount());
            } else if (dropObj instanceof de.bund.zrb.model.TestCase) {
                // … auf Case → nach diesem Case in dessen Suite
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dropTarget.getParent();
                if (parent == null) return null;
                return new MovePlan(parent, parent.getIndex(dropTarget) + 1);
            }
            return null;
        }

        // Suite wird gedroppt …
        if (movedObj instanceof de.bund.zrb.model.TestSuite) {
            if (dropObj == null) {
                // … auf Root → ans Ende
                return new MovePlan(dropTarget, dropTarget.getChildCount());
            } else if (dropObj instanceof de.bund.zrb.model.TestSuite) {
                // … auf Suite → nach dieser Suite im Root
                DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dropTarget.getParent();
                if (parent == null) return null;
                return new MovePlan(parent, parent.getIndex(dropTarget) + 1);
            }
            return null;
        }

        return null;
    }

    private int adjustForSameParent(DefaultMutableTreeNode oldParent,
                                    DefaultMutableTreeNode moved,
                                    int insertIndex) {
        if (oldParent == null) return insertIndex;
        int oldIndex = oldParent.getIndex(moved);
        if (oldIndex >= 0 && insertIndex > oldIndex) {
            return insertIndex - 1;
        }
        return insertIndex;
    }

    private void applyDomainMove(DefaultMutableTreeNode moved,
                                 DefaultMutableTreeNode newParentNode,
                                 int insertIndex) {
        Object movedObj = ((TestNode) moved).getModelRef();
        DefaultMutableTreeNode oldParentNode = (DefaultMutableTreeNode) moved.getParent();
        Object oldParentObj = oldParentNode instanceof TestNode ? ((TestNode) oldParentNode).getModelRef() : null;
        Object newParentObj = newParentNode instanceof TestNode ? ((TestNode) newParentNode).getModelRef() : null;

        if (movedObj instanceof de.bund.zrb.model.TestAction) {
            de.bund.zrb.model.TestAction action = (de.bund.zrb.model.TestAction) movedObj;

            if (oldParentObj instanceof de.bund.zrb.model.TestCase) {
                de.bund.zrb.model.TestCase oldCase = (de.bund.zrb.model.TestCase) oldParentObj;
                oldCase.getWhen().remove(action);
            }
            if (newParentObj instanceof de.bund.zrb.model.TestCase) {
                de.bund.zrb.model.TestCase newCase = (de.bund.zrb.model.TestCase) newParentObj;
                java.util.List<de.bund.zrb.model.TestAction> list = newCase.getWhen();
                list.add(clamp(insertIndex, list.size()), action);
            }
            return;
        }

        if (movedObj instanceof de.bund.zrb.model.TestCase) {
            de.bund.zrb.model.TestCase tc = (de.bund.zrb.model.TestCase) movedObj;

            if (oldParentObj instanceof de.bund.zrb.model.TestSuite) {
                de.bund.zrb.model.TestSuite oldSuite = (de.bund.zrb.model.TestSuite) oldParentObj;
                oldSuite.getTestCases().remove(tc);
            }
            if (newParentObj instanceof de.bund.zrb.model.TestSuite) {
                de.bund.zrb.model.TestSuite newSuite = (de.bund.zrb.model.TestSuite) newParentObj;
                java.util.List<de.bund.zrb.model.TestCase> list = newSuite.getTestCases();
                list.add(clamp(insertIndex, list.size()), tc);
            }
            return;
        }

        if (movedObj instanceof de.bund.zrb.model.TestSuite) {
            de.bund.zrb.model.TestSuite suite = (de.bund.zrb.model.TestSuite) movedObj;
            java.util.List<de.bund.zrb.model.TestSuite> list = de.bund.zrb.service.TestRegistry.getInstance().getAll();
            list.remove(suite);
            list.add(clamp(insertIndex, list.size()), suite);
        }
    }

    private int clamp(int idx, int size) {
        if (idx < 0) return 0;
        if (idx > size) return size;
        return idx;
    }

    private void applyTreeMove(JTree tree,
                               DefaultMutableTreeNode moved,
                               DefaultMutableTreeNode newParentNode,
                               int insertIndex) {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.removeNodeFromParent(moved);
        insertIndex = Math.max(0, Math.min(insertIndex, newParentNode.getChildCount()));
        model.insertNodeInto(moved, newParentNode, insertIndex);
        tree.expandPath(new TreePath(newParentNode.getPath()));
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        // Removal is handled in importData.  Only remove if the drag was aborted.
        if (action == MOVE && draggedNode != null) {
            JTree tree = (JTree) source;
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.removeNodeFromParent(draggedNode);
        }
        draggedNode = null;
    }

    private static class NodesTransferable implements Transferable {
        private final DefaultMutableTreeNode node;
        private final DataFlavor[] flavors;

        public NodesTransferable(DefaultMutableTreeNode node) {
            this.node = node;
            // Use the same JVM-local DataFlavor as the handler to avoid serialization
            String mimeType = DataFlavor.javaJVMLocalObjectMimeType + ";class=" + DefaultMutableTreeNode.class.getName();
            try {
                this.flavors = new DataFlavor[]{new DataFlavor(mimeType)};
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to create DataFlavor for Transferable", e);
            }
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return flavors[0].equals(flavor);
        }

        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
            return node;
        }
    }
}
