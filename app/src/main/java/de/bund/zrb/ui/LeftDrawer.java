package de.bund.zrb.ui;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.TestSuiteSavedEvent;
import de.bund.zrb.model.*;
import de.bund.zrb.service.TestPlayerService;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.commandframework.CommandRegistryImpl;
import de.bund.zrb.ui.commandframework.MenuCommand;
import de.bund.zrb.ui.tabs.ActionEditorTab;
import de.bund.zrb.ui.tabs.CaseEditorTab;
import de.bund.zrb.ui.tabs.SuiteEditorTab;
import de.bund.zrb.ui.tabs.UIHelper;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        refreshTestSuites(null);

        // 📌 Drag & Drop aktivieren:
        testTree.setDragEnabled(true);
        testTree.setDropMode(DropMode.ON_OR_INSERT);
        testTree.setTransferHandler(new TestSuiteTreeTransferHandler());
        testTree.setCellRenderer(new TestTreeCellRenderer());

        testTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    TreePath path = testTree.getPathForLocation(e.getX(), e.getY());
                    if (path != null) {
                        TestNode node = (TestNode) path.getLastPathComponent();
                        openEditorTab(node);
                    }
                }
            }
        });

        JScrollPane treeScroll = new JScrollPane(testTree);

        JButton playButton = new JButton("▶");
        playButton.setBackground(Color.GREEN);
        playButton.setFocusPainted(false);

        playButton.addActionListener(e -> {
            MenuCommand playCommand = commandRegistry.getById("testsuite.play").get();
            playCommand.perform();
        });

        setupContextMenu();

        add(playButton, BorderLayout.NORTH);
        add(treeScroll, BorderLayout.CENTER);

        ApplicationEventBus.getInstance().subscribe(event -> {
            if (event instanceof TestSuiteSavedEvent) {
                refreshTestSuites((String) event.getPayload()); // die Methode, um die Liste neu zu laden
            }
        });

        TestPlayerService.getInstance().registerDrawer(this);
    }

    private void refreshTestSuites(String name) {
        TestNode root = new TestNode("Testsuites");

        for (TestSuite suite : TestRegistry.getInstance().getAll()) {
            TestNode suiteNode = new TestNode(suite.getName(), suite);
            for (TestCase testCase : suite.getTestCases()) {
                TestNode caseNode = new TestNode(testCase.getName(), testCase);
                for (TestAction action : testCase.getWhen()) {
                    String label = action.getAction();
                    if (action.getValue() != null && !action.getValue().isEmpty()) {
                        label += " [" + action.getValue() + "]";
                    } else if (action.getSelectedSelector() != null) {
                        label += " [" + action.getSelectedSelector() + "]";
                    }
                    TestNode stepNode = new TestNode(label, action);
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

    /**
     * Set up a dynamic context menu for the test tree. Depending on the clicked node,
     * offer to create a new TestSuite, TestCase or Step (When), and preserve rename,
     * delete and properties actions. The new element will always be inserted directly
     * after the clicked element within its parent.
     */
    private void setupContextMenu() {
        // Remove any existing static popup menu
        testTree.setComponentPopupMenu(null);
        testTree.addMouseListener(new java.awt.event.MouseAdapter() {
            private void handlePopup(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int x = e.getX();
                int y = e.getY();
                TreePath path = testTree.getPathForLocation(x, y);
                TestNode clicked = null;
                if (path != null) {
                    Object n = path.getLastPathComponent();
                    if (n instanceof TestNode) {
                        clicked = (TestNode) n;
                        // Update selection so rename/delete targets the correct node
                        testTree.setSelectionPath(path);
                    }
                }
                JPopupMenu menu = buildContextMenu(clicked);
                menu.show(e.getComponent(), x, y);
            }
            @Override public void mousePressed(java.awt.event.MouseEvent e) { handlePopup(e); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { handlePopup(e); }
        });
    }

    /**
     * Build a context menu based on the clicked node. If the node is null
     * (blank area), offer to create a new TestSuite at root. For a TestSuite
     * node, offer to create another suite after it. For a TestCase, offer to
     * create a new TestCase after it. For a TestAction (step), offer to
     * create a new step after it. Rename, delete and properties actions are
     * included as appropriate.
     */
    private JPopupMenu buildContextMenu(TestNode clicked) {
        JPopupMenu menu = new JPopupMenu();
        if (clicked != null) {
            Object ref = clicked.getModelRef();
            if (ref instanceof TestAction) {
                JMenuItem newStep = new JMenuItem("Neuer Schritt");
                newStep.addActionListener(evt -> createNewStep(clicked));
                menu.add(newStep);
            } else if (ref instanceof TestCase) {
                JMenuItem newCase = new JMenuItem("Neuer TestCase");
                newCase.addActionListener(evt -> createNewCase(clicked));
                menu.add(newCase);
            } else if (ref instanceof TestSuite) {
                JMenuItem newSuite = new JMenuItem("Neue Testsuite");
                newSuite.addActionListener(evt -> createNewSuiteAfter(clicked));
                menu.add(newSuite);
            } else {
                JMenuItem newSuite = new JMenuItem("Neue Testsuite");
                newSuite.addActionListener(evt -> createNewSuiteAfter(null));
                menu.add(newSuite);
            }

            // Always show rename, delete (if not root), and properties
            menu.addSeparator();
            JMenuItem renameItem = new JMenuItem("Umbenennen");
            renameItem.addActionListener(evt -> renameNode());
            menu.add(renameItem);

            JMenuItem deleteItem = new JMenuItem("Löschen");
            deleteItem.addActionListener(evt -> deleteNode());
            menu.add(deleteItem);

            menu.addSeparator();
            JMenuItem propertiesItem = new JMenuItem("Eigenschaften");
            propertiesItem.addActionListener(evt -> openPropertiesDialog());
            menu.add(propertiesItem);
        } else {
            // Blank area: only new suite
            JMenuItem newSuite = new JMenuItem("Neue Testsuite");
            newSuite.addActionListener(evt -> createNewSuiteAfter(null));
            menu.add(newSuite);
        }
        return menu;
    }

    /**
     * Create a new TestCase directly after the given TestCase node. Updates both
     * the underlying model and the UI tree. Prompts the user for a name.
     *
     * @param caseNode the TestNode representing the existing TestCase after
     *                 which the new case should be inserted
     */
    private void createNewCase(TestNode caseNode) {
        if (caseNode == null || !(caseNode.getModelRef() instanceof TestCase)) return;
        String name = JOptionPane.showInputDialog(this, "Name des neuen TestCase:", "Neuer TestCase", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        TestCase oldCase = (TestCase) caseNode.getModelRef();
        TestNode suiteNode = (TestNode) caseNode.getParent();
        if (suiteNode == null || !(suiteNode.getModelRef() instanceof TestSuite)) return;
        TestSuite suite = (TestSuite) suiteNode.getModelRef();
        TestCase newCase = new TestCase(name, new java.util.ArrayList<>());
        // Insert into suite's list
        java.util.List<TestCase> cases = suite.getTestCases();
        int idx = cases.indexOf(oldCase);
        if (idx < 0) idx = cases.size() - 1;
        cases.add(idx + 1, newCase);
        // Insert into tree
        TestNode newCaseNode = new TestNode(name, newCase);
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.insertNodeInto(newCaseNode, suiteNode, idx + 1);
        testTree.expandPath(new TreePath(suiteNode.getPath()));
        // Persist
        TestRegistry.getInstance().save();
    }

    /**
     * Create a new step (When) directly after the given TestAction node. Prompts
     * the user for the action name and inserts the new action into the parent
     * TestCase and the UI tree.
     *
     * @param stepNode the TestNode representing the existing step after which
     *                 the new step should be inserted
     */
    private void createNewStep(TestNode stepNode) {
        if (stepNode == null || !(stepNode.getModelRef() instanceof TestAction)) return;
        String actionName = JOptionPane.showInputDialog(this, "Aktion für neuen Step (z. B. click, navigate):", "Neuer Step", JOptionPane.PLAIN_MESSAGE);
        if (actionName == null || actionName.trim().isEmpty()) return;
        TestAction oldAction = (TestAction) stepNode.getModelRef();
        TestNode caseNode = (TestNode) stepNode.getParent();
        if (caseNode == null || !(caseNode.getModelRef() instanceof TestCase)) return;
        TestCase testCase = (TestCase) caseNode.getModelRef();
        TestAction newAction = new TestAction(actionName);
        // Insert into case's when list
        java.util.List<TestAction> actions = testCase.getWhen();
        int idx = actions.indexOf(oldAction);
        if (idx < 0) idx = actions.size() - 1;
        actions.add(idx + 1, newAction);
        // Create UI node label similar to refreshTestSuites
        String label = newAction.getAction();
        TestNode newStepNode = new TestNode(label, newAction);
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.insertNodeInto(newStepNode, caseNode, idx + 1);
        testTree.expandPath(new TreePath(caseNode.getPath()));
        // Persist
        TestRegistry.getInstance().save();
    }

    /**
     * Create a new TestSuite directly after the clicked suite node (or at the
     * end of root if clicked is null). Updates the underlying TestRegistry and
     * inserts the new suite in the UI tree.
     *
     * @param clickedSuite the TestNode representing the suite after which the new
     *                     suite should be inserted, or null to append at root
     */
    private void createNewSuiteAfter(TestNode clickedSuite) {
        String name = JOptionPane.showInputDialog(this, "Name der neuen Testsuite:", "Neue Testsuite", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;
        // Determine parent node and insertion index in tree
        TestNode parentNode;
        int insertIndex;
        java.util.List<TestSuite> registryList = TestRegistry.getInstance().getAll();
        int registryInsertIndex;
        if (clickedSuite != null && clickedSuite.getModelRef() instanceof TestSuite) {
            // Insert after the clicked suite in the root list
            TestSuite oldSuite = (TestSuite) clickedSuite.getModelRef();
            registryInsertIndex = registryList.indexOf(oldSuite);
            if (registryInsertIndex < 0) registryInsertIndex = registryList.size() - 1;
            registryInsertIndex++;
            parentNode = (TestNode) clickedSuite.getParent();
            // In tree, insert as sibling after clickedSuite
            if (parentNode == null) {
                parentNode = (TestNode) testTree.getModel().getRoot();
            }
            insertIndex = parentNode.getIndex(clickedSuite) + 1;
        } else {
            // Append at the end of the root
            registryInsertIndex = registryList.size();
            parentNode = (TestNode) testTree.getModel().getRoot();
            insertIndex = parentNode.getChildCount();
        }
        TestSuite newSuite = new TestSuite(name, new java.util.ArrayList<>());
        registryList.add(registryInsertIndex, newSuite);
        // Create UI node
        TestNode newSuiteNode = new TestNode(name, newSuite);
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.insertNodeInto(newSuiteNode, parentNode, insertIndex);
        testTree.expandPath(new TreePath(parentNode.getPath()));
        TestRegistry.getInstance().save();
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

    void deleteNode() {
        TestNode selected = (TestNode) testTree.getLastSelectedPathComponent();
        if (selected == null || selected.getParent() == null) return;

        Object userObject = selected.getModelRef();
        Object parentObject = ((TestNode) selected.getParent()).getModelRef();

        if (userObject instanceof TestSuite) {
            TestRegistry.getInstance().getAll().remove(userObject);
        } else if (userObject instanceof TestCase && parentObject instanceof TestSuite) {
            ((TestSuite) parentObject).getTestCases().remove(userObject);
        } else if (parentObject instanceof TestCase) {
            TestCase testCase = (TestCase) parentObject;

            if (userObject instanceof TestAction) {
                testCase.getWhen().remove(userObject);
            } else if (userObject instanceof GivenCondition) {
                testCase.getGiven().remove(userObject);
            } else if (userObject instanceof ThenExpectation) {
                testCase.getThen().remove(userObject);
            }
        }

        ((DefaultTreeModel) testTree.getModel()).removeNodeFromParent(selected);
        ((DefaultTreeModel) testTree.getModel()).nodeStructureChanged((TestNode) selected.getParent());

        TestRegistry.getInstance().save();
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
        // Set the status on the node itself
        node.setStatus(passed ? TestNode.Status.PASSED : TestNode.Status.FAILED);
        // Notify the tree model that this node has changed
        ((DefaultTreeModel) testTree.getModel()).nodeChanged(node);
        // Propagate status up the tree: if a child fails, its parents should reflect the failure
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent instanceof TestNode) {
            updateSuiteStatus((TestNode) parent);
        }
    }

    @Override
    public void updateSuiteStatus(TestNode suite) {
        // Determine the status of a suite/case based on its children
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
        // Notify the model that the suite/case has changed
        ((DefaultTreeModel) testTree.getModel()).nodeChanged(suite);
        // Recursively propagate status changes upwards
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) suite.getParent();
        if (parent instanceof TestNode) {
            updateSuiteStatus((TestNode) parent);
        }
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

    private void openEditorTab(TestNode node) {
        Object ref = node.getModelRef();
        JComponent tab = null;
        String title = node.toString();

        if (ref instanceof TestAction) {
            tab = new ActionEditorTab((TestAction) ref);
        } else if (ref instanceof TestCase) {
            TestNode parent = (TestNode) node.getParent();
            Object suiteRef = parent.getModelRef();
            if (suiteRef instanceof TestSuite) {
                tab = new CaseEditorTab((TestSuite) suiteRef, (TestCase) ref);
            } else {
                tab = new CaseEditorTab(null, (TestCase) ref); // fallback
            }
        } else if (ref instanceof TestSuite) {
            tab = new SuiteEditorTab((TestSuite) ref);
        }

        if (tab != null) {
            Component parent = SwingUtilities.getWindowAncestor(this);
            if (parent instanceof JFrame) {
                JTabbedPane tabbedPane = UIHelper.findTabbedPane((JFrame) parent);
                if (tabbedPane != null) {
                    tabbedPane.addTab(title, tab);
                    tabbedPane.setSelectedComponent(tab);
                }
            }
        }
    }

}
