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

        JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
        TreePath destPath = dl.getPath();
        JTree tree = (JTree) support.getComponent();

        try {
            // Retrieve the node being dropped (reference, not serialized)
            DefaultMutableTreeNode droppedNode = (DefaultMutableTreeNode) support.getTransferable().getTransferData(nodesFlavor);
            if (droppedNode == null) {
                return false;
            }

            // Root should not be moved
            if (droppedNode.getParent() == null) {
                return false;
            }

            // Determine the target component and compute the insertion index
            DefaultMutableTreeNode dropTarget = destPath != null ? (DefaultMutableTreeNode) destPath.getLastPathComponent() : null;
            int childIndex = dl.getChildIndex();

            // Reject drops with no valid target
            if (dropTarget == null) {
                return false;
            }

            // Do not allow dropping a node onto one of its own descendants
            if (droppedNode.isNodeAncestor(dropTarget)) {
                return false;
            }

            // Compute the new parent and insertion index based on the types involved
            DefaultMutableTreeNode newParentNode;
            int insertIndex;

            Object droppedObj = ((TestNode) droppedNode).getModelRef();
            DefaultMutableTreeNode oldParentNode = (DefaultMutableTreeNode) droppedNode.getParent();
            Object oldParentObj = oldParentNode instanceof TestNode ? ((TestNode) oldParentNode).getModelRef() : null;
            Object dropObj = dropTarget instanceof TestNode ? ((TestNode) dropTarget).getModelRef() : null;

            if (dropObj == null) {
                // Dropping onto the root (which has no modelRef) – only suites allowed
                if (!(droppedObj instanceof de.bund.zrb.model.TestSuite)) {
                    return false;
                }
                newParentNode = dropTarget;
                insertIndex = (childIndex == -1) ? newParentNode.getChildCount() : childIndex;
            } else if (dropObj instanceof de.bund.zrb.model.TestSuite) {
                // Target is a suite node
                if (droppedObj instanceof de.bund.zrb.model.TestAction) {
                    // Prevent dropping actions directly into suites
                    return false;
                } else if (droppedObj instanceof de.bund.zrb.model.TestCase) {
                    // Move case into suite
                    newParentNode = dropTarget;
                    insertIndex = (childIndex == -1) ? newParentNode.getChildCount() : childIndex;
                } else if (droppedObj instanceof de.bund.zrb.model.TestSuite) {
                    // Reorder suites – new parent is the root
                    newParentNode = (DefaultMutableTreeNode) dropTarget.getParent();
                    if (newParentNode == null) {
                        return false;
                    }
                    int targetIndex = newParentNode.getIndex(dropTarget);
                    if (childIndex == -1) {
                        insertIndex = targetIndex + 1;
                    } else {
                        insertIndex = childIndex;
                    }
                } else {
                    return false;
                }
            } else if (dropObj instanceof de.bund.zrb.model.TestCase) {
                // Target is a case node
                if (droppedObj instanceof de.bund.zrb.model.TestAction) {
                    // Dropping an action into a test case (move or reorder)
                    newParentNode = dropTarget;
                    insertIndex = (childIndex == -1) ? newParentNode.getChildCount() : childIndex;
                } else if (droppedObj instanceof de.bund.zrb.model.TestCase) {
                    // Dropping a case onto another case means reorder within its parent (suite)
                    newParentNode = (DefaultMutableTreeNode) dropTarget.getParent();
                    if (newParentNode == null) {
                        return false;
                    }
                    int targetIndex = newParentNode.getIndex(dropTarget);
                    insertIndex = (childIndex == -1) ? targetIndex + 1 : childIndex;
                } else if (droppedObj instanceof de.bund.zrb.model.TestSuite) {
                    // Prevent dropping a suite into a case
                    return false;
                } else {
                    return false;
                }
            } else if (dropObj instanceof de.bund.zrb.model.TestAction) {
                // Target is an action node
                if (!(droppedObj instanceof de.bund.zrb.model.TestAction)) {
                    // Only actions can be reordered relative to actions
                    return false;
                }
                // Determine parent (the case) and insert relative to the target
                newParentNode = (DefaultMutableTreeNode) dropTarget.getParent();
                if (newParentNode == null) {
                    return false;
                }
                int targetIndex = newParentNode.getIndex(dropTarget);
                insertIndex = (childIndex == -1) ? targetIndex + 1 : childIndex;
            } else {
                // Unknown target type
                return false;
            }

            // Adjust insertion index if moving within the same parent and inserting after removal
            if (newParentNode.equals(oldParentNode)) {
                int oldIndex = oldParentNode.getIndex(droppedNode);
                if (insertIndex > oldIndex) {
                    insertIndex--;
                }
            }

            // Update the underlying data model based on what is being moved
            if (droppedObj instanceof de.bund.zrb.model.TestAction) {
                de.bund.zrb.model.TestAction action = (de.bund.zrb.model.TestAction) droppedObj;
                // Remove from old test case
                if (oldParentObj instanceof de.bund.zrb.model.TestCase) {
                    de.bund.zrb.model.TestCase oldCase = (de.bund.zrb.model.TestCase) oldParentObj;
                    oldCase.getWhen().remove(action);
                }
                // Add to new test case
                Object newParentObj = newParentNode instanceof TestNode ? ((TestNode) newParentNode).getModelRef() : null;
                if (newParentObj instanceof de.bund.zrb.model.TestCase) {
                    de.bund.zrb.model.TestCase newCase = (de.bund.zrb.model.TestCase) newParentObj;
                    java.util.List<de.bund.zrb.model.TestAction> list = newCase.getWhen();
                    if (insertIndex < 0 || insertIndex > list.size()) {
                        insertIndex = list.size();
                    }
                    list.add(insertIndex, action);
                }
            } else if (droppedObj instanceof de.bund.zrb.model.TestCase) {
                de.bund.zrb.model.TestCase testCase = (de.bund.zrb.model.TestCase) droppedObj;
                // Remove from old suite
                if (oldParentObj instanceof de.bund.zrb.model.TestSuite) {
                    de.bund.zrb.model.TestSuite oldSuite = (de.bund.zrb.model.TestSuite) oldParentObj;
                    oldSuite.getTestCases().remove(testCase);
                }
                // Add to new suite
                Object newParentObj = newParentNode instanceof TestNode ? ((TestNode) newParentNode).getModelRef() : null;
                if (newParentObj instanceof de.bund.zrb.model.TestSuite) {
                    de.bund.zrb.model.TestSuite newSuite = (de.bund.zrb.model.TestSuite) newParentObj;
                    java.util.List<de.bund.zrb.model.TestCase> list = newSuite.getTestCases();
                    if (insertIndex < 0 || insertIndex > list.size()) {
                        insertIndex = list.size();
                    }
                    list.add(insertIndex, testCase);
                }
            } else if (droppedObj instanceof de.bund.zrb.model.TestSuite) {
                de.bund.zrb.model.TestSuite suite = (de.bund.zrb.model.TestSuite) droppedObj;
                java.util.List<de.bund.zrb.model.TestSuite> list = de.bund.zrb.service.TestRegistry.getInstance().getAll();
                list.remove(suite);
                if (insertIndex < 0 || insertIndex > list.size()) {
                    insertIndex = list.size();
                }
                list.add(insertIndex, suite);
            }

            // Update tree model
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.removeNodeFromParent(droppedNode);
            model.insertNodeInto(droppedNode, newParentNode, insertIndex);
            tree.expandPath(new TreePath(newParentNode.getPath()));

            // Persist changes
            de.bund.zrb.service.TestRegistry.getInstance().save();

            // Prevent exportDone from removing again
            draggedNode = null;

            return true;

        } catch (UnsupportedFlavorException | IOException ex) {
            ex.printStackTrace();
        }
        return false;
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
