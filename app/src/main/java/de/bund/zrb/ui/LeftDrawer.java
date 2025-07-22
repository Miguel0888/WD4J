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
        testTree = new JTree();
        refreshTestSuites();
        testTree.setDragEnabled(true);
        testTree.setDropMode(DropMode.ON_OR_INSERT);
        testTree.setTransferHandler(new TestSuiteTreeTransferHandler());
        testTree.setCellRenderer(new TestTreeCellRenderer());
        JScrollPane treeScroll = new JScrollPane(testTree);
        JPopupMenu contextMenu = new JPopupMenu();
        JMenuItem delete = new JMenuItem("Löschen");
        delete.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                deleteNode();
            }
        });
        contextMenu.add(delete);
        testTree.setComponentPopupMenu(contextMenu);
        add(treeScroll, BorderLayout.CENTER);
    }

    private void deleteNode() {
        TestNode selected = (TestNode) testTree.getLastSelectedPathComponent();
        if (selected != null && selected.getParent() != null) {
            Object userObject = selected.getUserObject();
            if (userObject instanceof TestSuite) {
                TestRegistry.getInstance().getAll().remove(userObject);
            } else if (userObject instanceof TestCase) {
                TestNode parentNode = (TestNode) selected.getParent();
                Object parentObj = parentNode.getUserObject();
                if (parentObj instanceof TestSuite) {
                    ((TestSuite) parentObj).getTestCases().remove(userObject);
                }
            } else if (userObject instanceof TestAction) {
                TestNode caseNode = (TestNode) selected.getParent();
                Object caseObj = caseNode.getUserObject();
                if (caseObj instanceof TestCase) {
                    ((TestCase) caseObj).getWhen().remove(userObject);
                }
            }
            DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
            TestNode parent = (TestNode) selected.getParent();
            model.removeNodeFromParent(selected);
            model.nodeStructureChanged(parent);
            TestRegistry.getInstance().save();
        }
    }

    private void refreshTestSuites() {
        TestNode root = new TestNode("Testsuites");
        for (TestSuite suite : TestRegistry.getInstance().getAll()) {
            TestNode suiteNode = new TestNode(suite);
            for (TestCase testCase : suite.getTestCases()) {
                TestNode caseNode = new TestNode(testCase);
                for (TestAction action : testCase.getWhen()) {
                    caseNode.add(new TestNode(action));
                }
                suiteNode.add(caseNode);
            }
            root.add(suiteNode);
        }
        DefaultTreeModel model = new DefaultTreeModel(root);
        testTree.setModel(model);
    }

    @Override
    public TestNode getSelectedNode() {
        return (TestNode) testTree.getLastSelectedPathComponent();
    }

    @Override
    public TestNode getRootNode() {
        return (TestNode) ((DefaultTreeModel) testTree.getModel()).getRoot();
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
    public List<TestSuite> getSelectedSuites() {
        TreePath[] paths = testTree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return TestRegistry.getInstance().getAll();
        }
        List<TestSuite> selected = new ArrayList<>();
        for (TreePath path : paths) {
            Object node = path.getLastPathComponent();
            if (node instanceof TestNode) {
                Object obj = ((TestNode) node).getUserObject();
                if (obj instanceof TestSuite) {
                    selected.add((TestSuite) obj);
                }
            }
        }
        if (selected.isEmpty()) {
            return TestRegistry.getInstance().getAll();
        }
        return selected;
    }
}
