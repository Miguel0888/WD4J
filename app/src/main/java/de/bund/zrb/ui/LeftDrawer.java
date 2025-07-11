package de.bund.zrb.ui;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.TestAction;
import de.bund.zrb.model.TestCase;
import de.bund.zrb.model.TestSuite;
import de.bund.zrb.service.TestPlayerService;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.commandframework.CommandRegistryImpl;
import de.bund.zrb.ui.commandframework.MenuCommand;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Left drawer: tree view + green play button + drag & drop.
 */
public class LeftDrawer extends JPanel implements TestPlayerUi {

    private final CommandRegistryImpl commandRegistry = CommandRegistryImpl.getInstance();
    private final JTree testTree;

    public LeftDrawer() {
        super(new BorderLayout());

        testTree = getTreeData();
        refreshTestSuites();

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

        ApplicationEventBus.getInstance().subscribe(event -> {
            if (event instanceof TestSuiteSavedEvent) {
                refreshTestSuites(); // deine Methode, um die linke Liste neu zu laden
            }
        });

        TestPlayerService.getInstance().registerDrawer(this);
    }

    private void refreshTestSuites() {
        TestNode root = new TestNode("Testsuites");

        for (TestSuite suite : TestRegistry.getInstance().getAll()) {
            TestNode suiteNode = new TestNode(suite.getName());
            for (TestCase testCase : suite.getTestCases()) {
                TestNode caseNode = new TestNode(testCase.getName());
                for (TestAction action : testCase.getWhen()) {
                    String label = action.getAction();
                    if (action.getValue() != null && !action.getValue().isEmpty()) {
                        label += " [" + action.getValue() + "]";
                    } else if (action.getSelectedSelector() != null) {
                        label += " [" + action.getSelectedSelector() + "]";
                    }
                    TestNode stepNode = new TestNode(label);
                    caseNode.add(stepNode);
                }
                suiteNode.add(caseNode);
            }
            root.add(suiteNode);
        }

        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.setRoot(root);
        model.reload();
    }

    private JTree getTreeData() {
        TestNode root = new TestNode("Testsuites");
        JTree tree = new JTree(root);
        tree.setCellRenderer(new TestTreeCellRenderer());
        return tree;
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

    @Override
    public TestNode getSelectedNode() {
        return (TestNode) testTree.getLastSelectedPathComponent();
    }

    @Override
    public void updateNodeStatus(TestNode node, boolean passed) {
        node.setStatus(passed ? TestNode.Status.PASSED : TestNode.Status.FAILED);
        ((DefaultTreeModel) testTree.getModel()).nodeChanged(node);
    }

    @Override
    public void updateSuiteStatus(TestNode suite) {
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
    @Override
    public TestNode getRootNode() {
        return (TestNode) testTree.getModel().getRoot();
    }


    private DefaultMutableTreeNode getSelectedNodeOrRoot() {
        DefaultMutableTreeNode selected = getSelectedNode();
        return selected != null ? selected : (DefaultMutableTreeNode) testTree.getModel().getRoot();
    }

    @Override
    public List<TestSuite> getSelectedSuites() {
        // Wenn Root markiert: alles
        // Wenn Suites markiert: nur die
        TreePath[] paths = testTree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return TestRegistry.getInstance().getAll();
        }

        List<TestSuite> selected = new ArrayList<>();
        for (TreePath path : paths) {
            Object node = path.getLastPathComponent();
            if (node instanceof TestNode) {
                String suiteName = ((TestNode) node).toString();
                for (TestSuite suite : TestRegistry.getInstance().getAll()) {
                    if (suite.getName().equals(suiteName)) {
                        selected.add(suite);
                    }
                }
            }
        }
        if (selected.isEmpty()) {
            return TestRegistry.getInstance().getAll();
        }
        return selected;
    }

}
