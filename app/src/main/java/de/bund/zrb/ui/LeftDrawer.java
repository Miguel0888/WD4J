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
        testTree.addMouseListener(new java.awt.event.MouseAdapter() {
            private void maybeShow(java.awt.event.MouseEvent e) {
                if (!e.isPopupTrigger()) return;

                int x = e.getX(), y = e.getY();
                TreePath path = testTree.getPathForLocation(x, y);
                if (path != null) testTree.setSelectionPath(path);

                TestNode node = null;
                if (path != null && path.getLastPathComponent() instanceof TestNode) {
                    node = (TestNode) path.getLastPathComponent();
                }

                JPopupMenu menu = buildContextMenu(node);
                menu.show(testTree, x, y);
            }

            @Override public void mousePressed(java.awt.event.MouseEvent e) { maybeShow(e); }
            @Override public void mouseReleased(java.awt.event.MouseEvent e) { maybeShow(e); }
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
    private JPopupMenu buildContextMenu(TestNode node) {
        JPopupMenu menu = new JPopupMenu();

        Object ref = (node != null) ? node.getModelRef() : null;

        // Root/Leerer Bereich oder Suite: nur "Neue Testsuite"
        if (ref == null || ref instanceof de.bund.zrb.model.TestSuite) {
            JMenuItem miNewSuite = new JMenuItem("Neue Testsuite");
            miNewSuite.addActionListener(a -> {
                if (ref instanceof de.bund.zrb.model.TestSuite) {
                    createNewSuiteAfter(node);
                } else {
                    createNewSuiteAfter(null); // am Ende unter Root
                }
            });
            menu.add(miNewSuite);

            if (ref instanceof de.bund.zrb.model.TestSuite) {
                JMenuItem miDuplicateSuite = new JMenuItem("Kopie von Testsuite");
                miDuplicateSuite.addActionListener(a -> duplicateSuiteAfter(node)); // ToDo: Missing Method
                menu.add(miDuplicateSuite);
            }
        }

        // Case: nur "Neuer TestCase" + "Kopie von TestCase"
        if (ref instanceof de.bund.zrb.model.TestCase) {
            JMenuItem miNewCase = new JMenuItem("Neuer TestCase");
            miNewCase.addActionListener(a -> createNewCaseAfter(node));
            menu.add(miNewCase);

            JMenuItem miDuplicateCase = new JMenuItem("Kopie von TestCase");
            miDuplicateCase.addActionListener(a -> duplicateCaseAfter(node)); // ToDo: Missing Method
            menu.add(miDuplicateCase);
        }

        // When-Step: nur "Neuer Schritt" + "Kopie von Schritt"
        if (ref instanceof de.bund.zrb.model.TestAction) {
            JMenuItem miNewStep = new JMenuItem("Neuer Schritt (When)");
            miNewStep.addActionListener(a -> createNewActionAfter(node));
            menu.add(miNewStep);

            JMenuItem miDuplicateStep = new JMenuItem("Kopie von Schritt");
            miDuplicateStep.addActionListener(a -> duplicateActionAfter(node)); // ToDo: Missing Method
            menu.add(miDuplicateStep);
        }

        // (Optional) bestehende Einträge wie Umbenennen/Löschen/Eigenschaften hier anhängen …

        return menu;
    }

    /**
     * Create a new TestSuite directly after the clicked suite node (or at the
     * end of root if clicked is null). Updates the underlying TestRegistry and
     * inserts the new suite in the UI tree.
     *
     * @param clickedSuite the TestNode representing the suite after which the new
     *                     suite should be inserted, or null to append at root
     */
    private void createNewSuiteAfter(TestNode clickedSuiteOrNull) {
        java.util.List<de.bund.zrb.model.TestSuite> suites = de.bund.zrb.service.TestRegistry.getInstance().getAll();
        int insertIndex = suites.size();
        if (clickedSuiteOrNull != null) {
            insertIndex = ((DefaultMutableTreeNode) clickedSuiteOrNull.getParent()).getIndex(clickedSuiteOrNull) + 1;
        }

        String baseName = "Neue Suite";
        String name = uniqueSuiteName(baseName);
        de.bund.zrb.model.TestSuite suite = new de.bund.zrb.model.TestSuite(); // ToDo: Missing Constructor
        suite.setName(name);

        suites.add(insertIndex, suite);
        de.bund.zrb.service.TestRegistry.getInstance().save();

        // Tree
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        TestNode root = (TestNode) model.getRoot();
        TestNode newNode = new TestNode(name, suite);
        model.insertNodeInto(newNode, root, Math.min(insertIndex, root.getChildCount()));
        selectNode(newNode);
    }

    private void createNewCaseAfter(TestNode clickedCase) {
        if (clickedCase == null) return;
        DefaultMutableTreeNode suiteNode = (DefaultMutableTreeNode) clickedCase.getParent();
        Object suiteRef = (suiteNode instanceof TestNode) ? ((TestNode) suiteNode).getModelRef() : null;
        if (!(suiteRef instanceof de.bund.zrb.model.TestSuite)) return;

        de.bund.zrb.model.TestSuite suite = (de.bund.zrb.model.TestSuite) suiteRef;
        java.util.List<de.bund.zrb.model.TestCase> cases = suite.getTestCases();
        int oldIdx = suiteNode.getIndex(clickedCase);
        int insertIndex = oldIdx + 1;

        String baseName = "Neuer Case";
        String name = uniqueCaseName(suite, baseName);

        de.bund.zrb.model.TestCase tc = new de.bund.zrb.model.TestCase(); // ToDo: Missing Constructor
        tc.setName(name);

        cases.add(insertIndex, tc);
        de.bund.zrb.service.TestRegistry.getInstance().save();

        // Tree
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        TestNode newNode = new TestNode(name, tc);
        model.insertNodeInto(newNode, (DefaultMutableTreeNode) suiteNode, Math.min(insertIndex, suiteNode.getChildCount()));
        selectNode(newNode);
    }

    private void createNewActionAfter(TestNode clickedAction) {
        if (clickedAction == null) return;
        DefaultMutableTreeNode caseNode = (DefaultMutableTreeNode) clickedAction.getParent();
        Object caseRef = (caseNode instanceof TestNode) ? ((TestNode) caseNode).getModelRef() : null;
        if (!(caseRef instanceof de.bund.zrb.model.TestCase)) return;

        de.bund.zrb.model.TestCase tc = (de.bund.zrb.model.TestCase) caseRef;
        java.util.List<de.bund.zrb.model.TestAction> steps = tc.getWhen();
        int oldIdx = caseNode.getIndex(clickedAction);
        int insertIndex = oldIdx + 1;

        de.bund.zrb.model.TestAction action = new de.bund.zrb.model.TestAction();
        action.setType(TestAction.ActionType.WHEN);
        action.setAction("click");                 // sinnvoller Default
        action.setTimeout(30_000);                 // Default-Timeout
        action.setUser("default");

        steps.add(insertIndex, action);
        de.bund.zrb.service.TestRegistry.getInstance().save();

        // Tree
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        TestNode newNode = new TestNode(String.valueOf(action), action);
        model.insertNodeInto(newNode, (DefaultMutableTreeNode) caseNode, Math.min(insertIndex, caseNode.getChildCount()));
        selectNode(newNode);
    }

    // in LeftDrawer.java
    private de.bund.zrb.model.TestCase cloneCaseDeep(de.bund.zrb.model.TestCase src, boolean prefixedName) {
        de.bund.zrb.model.TestCase c = new de.bund.zrb.model.TestCase(); // ToDo: Missing Constructor
        String name = safeName(src.getName());
        c.setName(prefixedName ? "copy of " + name : name);

        // Given (flach kopieren)
        if (src.getGiven() != null) {
            c.setGiven(new java.util.ArrayList<>(src.getGiven())); // ToDo: Should use new Copy Constructor
        }

        // When (Actions tief = neue Objekte)
        java.util.List<de.bund.zrb.model.TestAction> list = new java.util.ArrayList<>();
        if (src.getWhen() != null) {
            for (de.bund.zrb.model.TestAction a : src.getWhen()) {
                list.add(cloneActionShallow(a, false)); // bei Case-Copy kein "copy of" pro Step
            }
        }
        c.setWhen(list); // ToDo: Should use new Copy Constructor
        return c;
    }

    private de.bund.zrb.model.TestAction cloneActionShallow(de.bund.zrb.model.TestAction src, boolean tryPrefixName) {
        de.bund.zrb.model.TestAction a = new de.bund.zrb.model.TestAction();
        // bekannte Felder übernehmen (best effort)
        a.setType(src.getType());
        a.setAction(src.getAction());
        a.setValue(src.getValue());
        a.setUser(src.getUser());
        a.setTimeout(src.getTimeout());
        a.setSelectedSelector(src.getSelectedSelector());
        if (src.getSelectors() != null) { // ToDo: Missing Method getSelectors
            a.setSelectors(new java.util.ArrayList<>(src.getSelectors())); // ToDo: Missing Method getSelectors
        }
        // falls es einen Namen gibt, versuche „copy of …“
        if (tryPrefixName) {
            String n = getNameIfExists(src);
            if (n != null && !n.isEmpty()) {
                setNameIfExists(a, "copy of " + n);
            }
        }
        return a;
    }

    private String safeName(String s) {
        return (s == null || s.trim().isEmpty()) ? "unnamed" : s.trim();
    }

    private String uniqueSuiteName(String base) {
        java.util.List<de.bund.zrb.model.TestSuite> suites = de.bund.zrb.service.TestRegistry.getInstance().getAll();
        java.util.Set<String> used = new java.util.HashSet<>();
        for (de.bund.zrb.model.TestSuite s : suites) used.add(safeName(s.getName()));
        return makeUnique(base, used);
    }

    private String uniqueCaseName(de.bund.zrb.model.TestSuite suite, String base) {
        java.util.Set<String> used = new java.util.HashSet<>();
        for (de.bund.zrb.model.TestCase c : suite.getTestCases()) used.add(safeName(c.getName()));
        return makeUnique(base, used);
    }

    private String makeUnique(String base, java.util.Set<String> used) {
        String b = safeName(base);
        if (!used.contains(b)) return b;
        for (int i = 2; i < 10_000; i++) {
            String cand = b + " (" + i + ")";
            if (!used.contains(cand)) return cand;
        }
        return b + " (copy)"; // Fallback
    }

    // Name-Getter/Setter "best effort" (für TestAction optional vorhanden)
    private String getNameIfExists(Object bean) {
        try {
            java.lang.reflect.Method m = bean.getClass().getMethod("getName");
            Object v = m.invoke(bean);
            return (v != null) ? String.valueOf(v) : null;
        } catch (Exception ignore) { return null; }
    }

    private void setNameIfExists(Object bean, String name) {
        try {
            java.lang.reflect.Method m = bean.getClass().getMethod("setName", String.class);
            m.invoke(bean, name);
        } catch (Exception ignore) { /* kein Name vorhanden */ }
    }

    // Komfort: Node selektieren/anzeigen
    private void selectNode(TestNode node) {
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        javax.swing.tree.TreePath path = new javax.swing.tree.TreePath(node.getPath());
        testTree.expandPath(new javax.swing.tree.TreePath(((DefaultMutableTreeNode) node.getParent()).getPath()));
        testTree.setSelectionPath(path);
        testTree.scrollPathToVisible(path);
        model.nodeChanged(node);
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
