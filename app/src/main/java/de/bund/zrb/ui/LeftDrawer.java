package de.bund.zrb.ui;

import de.bund.zrb.ui.commandframework.CommandRegistry;
import de.bund.zrb.ui.commandframework.CommandRegistryImpl;
import de.bund.zrb.ui.commandframework.MenuCommand;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Left drawer: tree view + green play button + drag & drop.
 */
public class LeftDrawer extends JPanel {

    private final CommandRegistryImpl commandRegistry = CommandRegistryImpl.getInstance();
    private final JTree testTree;

    public LeftDrawer() {
        super(new BorderLayout());

        testTree = getTreeData();

        // 📌 Drag & Drop aktivieren:
        testTree.setDragEnabled(true);
        testTree.setDropMode(DropMode.ON_OR_INSERT);
        testTree.setTransferHandler(new TestSuiteTreeTransferHandler());
        testTree.setCellRenderer(new TestTreeCellRenderer());

        JScrollPane treeScroll = new JScrollPane(testTree);

        JButton playButton = new JButton("▶");
        playButton.setBackground(Color.GREEN);
        playButton.setFocusPainted(false);

        playButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MenuCommand playCommand = commandRegistry.getById("testsuite.play").get();
                playCommand.perform();
            }
        });

        setupContextMenu();

        add(playButton, BorderLayout.NORTH);
        add(treeScroll, BorderLayout.CENTER);
    }

    private JTree getTreeData() {
        final JTree testTree;
        TestNode root = new TestNode("Testsuites");
        TestNode suite1 = new TestNode("Suite 1");
        suite1.add(new TestNode("Test 1.1"));
        suite1.add(new TestNode("Test 1.2"));

        TestNode suite2 = new TestNode("Suite 2");
        suite2.add(new TestNode("Test 2.1"));

        root.add(suite1);
        root.add(suite2);

        testTree = new JTree(root);
        testTree.setCellRenderer(new TestTreeCellRenderer());

        return testTree;
    }

    private void setupContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();

        JMenuItem newSuite = new JMenuItem("Neue Testsuite");
        JMenuItem rename = new JMenuItem("Umbenennen");
        JMenuItem delete = new JMenuItem("Löschen");
        JMenuItem properties = new JMenuItem("Eigenschaften");

        newSuite.addActionListener(e -> createNewSuite());
        rename.addActionListener(e -> renameNode());
        delete.addActionListener(e -> deleteNode());
        properties.addActionListener(e -> openPropertiesDialog());

        contextMenu.add(newSuite);
        contextMenu.add(rename);
        contextMenu.add(delete);
        contextMenu.addSeparator();
        contextMenu.add(properties);

        testTree.setComponentPopupMenu(contextMenu);
    }

    private void createNewSuite() {
        String name = JOptionPane.showInputDialog(this, "Name der neuen Testsuite:", "Neue Testsuite", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            DefaultMutableTreeNode selected = getSelectedNodeOrRoot();
            DefaultMutableTreeNode newSuite = new DefaultMutableTreeNode(name);
            DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
            model.insertNodeInto(newSuite, selected, selected.getChildCount());
        }
    }

    private void renameNode() {
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null && selected.getParent() != null) { // Root nicht umbenennen
            String name = JOptionPane.showInputDialog(this, "Neuer Name:", selected.toString());
            if (name != null && !name.trim().isEmpty()) {
                selected.setUserObject(name);
                ((DefaultTreeModel) testTree.getModel()).nodeChanged(selected);
            }
        }
    }

    private void deleteNode() {
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null && selected.getParent() != null) { // Root nicht löschen
            ((DefaultTreeModel) testTree.getModel()).removeNodeFromParent(selected);
        }
    }

    private void openPropertiesDialog() {
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null && selected.getParent() != null) {
            PropertiesDialog dialog = new PropertiesDialog(selected.toString());
            dialog.setVisible(true);
        }
    }

    private DefaultMutableTreeNode getSelectedNode() {
        return (DefaultMutableTreeNode) testTree.getLastSelectedPathComponent();
    }

    private DefaultMutableTreeNode getSelectedNodeOrRoot() {
        DefaultMutableTreeNode selected = getSelectedNode();
        return selected != null ? selected : (DefaultMutableTreeNode) testTree.getModel().getRoot();
    }

    private void runTest(TestNode node) {
        // Blatt oder Suite?
        if (node.getChildCount() == 0) {
            simulateResult(node);
        } else {
            // Alle Kinder durchlaufen
            for (int i = 0; i < node.getChildCount(); i++) {
                runTest((TestNode) node.getChildAt(i));
            }
        }

        // Danach Suite-Status berechnen
        SwingUtilities.invokeLater(() -> updateSuiteStatus(node));
    }

    private void simulateResult(TestNode node) {
        Timer timer = new Timer(1000, null); // 1 Sekunde
        timer.addActionListener(e -> {
            boolean pass = Math.random() > 0.3; // 70% pass
            node.setStatus(pass ? TestNode.Status.PASSED : TestNode.Status.FAILED);

            ((DefaultTreeModel) testTree.getModel()).nodeChanged(node);
            timer.stop();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void updateSuiteStatus(TestNode suite) {
        if (suite.getChildCount() == 0) return;

        boolean hasFail = false;
        for (int i = 0; i < suite.getChildCount(); i++) {
            TestNode child = (TestNode) suite.getChildAt(i);
            if (child.getStatus() == TestNode.Status.FAILED) {
                hasFail = true;
                break;
            }
        }

        suite.setStatus(hasFail ? TestNode.Status.FAILED : TestNode.Status.PASSED);
        ((DefaultTreeModel) testTree.getModel()).nodeChanged(suite);
    }

}
