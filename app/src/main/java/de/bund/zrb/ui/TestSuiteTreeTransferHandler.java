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

    private final DataFlavor nodesFlavor;
    private DefaultMutableTreeNode draggedNode; // Save reference for exportDone

    public TestSuiteTreeTransferHandler() {
        nodesFlavor = new DataFlavor(DefaultMutableTreeNode.class, "Node");
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
        TreePath dest = dl.getPath();
        JTree tree = (JTree) support.getComponent();

        try {
            DefaultMutableTreeNode droppedNode = (DefaultMutableTreeNode) support.getTransferable().getTransferData(nodesFlavor);
            DefaultMutableTreeNode newParent = (DefaultMutableTreeNode) dest.getLastPathComponent();

            if (droppedNode.isNodeAncestor(newParent)) {
                // Prevent dropping into self/child
                return false;
            }

            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.insertNodeInto(droppedNode, newParent, newParent.getChildCount());
            tree.expandPath(dest);

            return true;

        } catch (UnsupportedFlavorException | IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
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
            this.flavors = new DataFlavor[]{new DataFlavor(DefaultMutableTreeNode.class, "Node")};
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
