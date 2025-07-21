package de.bund.zrb.ui;

// imports...

public class LeftDrawer extends JPanel implements TestPlayerUi {

    private final CommandRegistryImpl commandRegistry = CommandRegistryImpl.getInstance();
    private final JTree testTree;

    public LeftDrawer() {
        super(new BorderLayout());
        testTree = getTreeData();
        refreshTestSuites();
        testTree.setDragEnabled(true);
        testTree.setDropMode(DropMode.ON_OR_INSERT);
        testTree.setTransferHandler(new TestSuiteTreeTransferHandler());
        testTree.setCellRenderer(new TestTreeCellRenderer());
        JScrollPane treeScroll = new JScrollPane(testTree);
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem delete = new JMenuItem("Löschen");
        delete.addActionListener(e -> deleteNode());
        contextMenu.add(delete);
        testTree.setComponentPopupMenu(contextMenu);
        add(treeScroll, BorderLayout.CENTER);
    }

    private void deleteNode() {
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null && selected.getParent() != null) {
            DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
            model.removeNodeFromParent(selected);
            model.reload(); // Ensure the tree UI is refreshed after deletion
            TestTreeModel treeModel = (TestTreeModel) testTree.getModel();
            treeModel.save(); // Persist the modified tree structure
        }
    }

    private DefaultMutableTreeNode getSelectedNode() {
        return (DefaultMutableTreeNode) testTree.getLastSelectedPathComponent();
    }

    // Other methods omitted for brevity
}
