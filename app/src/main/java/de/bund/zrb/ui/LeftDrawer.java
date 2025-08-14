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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
                    String label = renderActionLabel(action);
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

    private String renderActionLabel(TestAction action) {
        String label = action.getAction();
        if (action.getValue() != null && !action.getValue().isEmpty()) {
            label += " [" + action.getValue() + "]";
        } else if (action.getSelectedSelector() != null && !action.getSelectedSelector().isEmpty()) {
            label += " [" + action.getSelectedSelector() + "]";
        }
        return label;
    }

    private JTree getTreeData() {
        TestNode root = new TestNode("Testsuites");
        JTree tree = new JTree(root);
        tree.setCellRenderer(new TestTreeCellRenderer());
        return tree;
    }

    /**
     * Set up a dynamic context menu for the test tree. Depending on the clicked node,
     * offer to create a new TestSuite, TestCase or Step (When) – and offer "Kopie erstellen".
     * Rename, delete and properties actions are preserved. New/copied elements are inserted
     * directly AFTER the clicked element within its parent (or at root if empty area).
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
     * Build a context menu based on the clicked node.
     * - Blank area: only new suite.
     * - Suite: new suite after + copy suite; then common items.
     * - Case: new case after + copy case; then common items.
     * - Step: new step after + copy step; then common items.
     */
    private JPopupMenu buildContextMenu(TestNode clicked) {
        JPopupMenu menu = new JPopupMenu();

        if (clicked == null) {
            JMenuItem newSuite = new JMenuItem("Neue Testsuite");
            newSuite.addActionListener(evt -> createNewSuiteAfter(null));
            menu.add(newSuite);
            return menu;
        }

        Object ref = clicked.getModelRef();

        if (ref instanceof TestSuite) {
            JMenuItem newSuite = new JMenuItem("Neue Testsuite");
            newSuite.addActionListener(evt -> createNewSuiteAfter(clicked));
            menu.add(newSuite);

            JMenuItem dupSuite = new JMenuItem("Kopie von Testsuite");
            dupSuite.addActionListener(evt -> duplicateSuiteAfter(clicked));
            menu.add(dupSuite);

            menu.addSeparator();
            addCommonMenuItems(menu, clicked);
            return menu;
        }

        if (ref instanceof TestCase) {
            JMenuItem newCase = new JMenuItem("Neuer TestCase");
            newCase.addActionListener(evt -> createNewCase(clicked));
            menu.add(newCase);

            JMenuItem dupCase = new JMenuItem("Kopie von TestCase");
            dupCase.addActionListener(evt -> duplicateCaseAfter(clicked));
            menu.add(dupCase);

            menu.addSeparator();
            addCommonMenuItems(menu, clicked);
            return menu;
        }

        if (ref instanceof TestAction) {
            JMenuItem newStep = new JMenuItem("Neuer Schritt");
            newStep.addActionListener(evt -> createNewStep(clicked));
            menu.add(newStep);

            JMenuItem dupStep = new JMenuItem("Kopie von Schritt");
            dupStep.addActionListener(evt -> duplicateActionAfter(clicked));
            menu.add(dupStep);

            menu.addSeparator();
            addCommonMenuItems(menu, clicked);
            return menu;
        }

        // Fallback
        addCommonMenuItems(menu, clicked);
        return menu;
    }

    private void addCommonMenuItems(JPopupMenu menu, TestNode onNode) {
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
    }

    /**
     * Create a new TestCase directly after the given TestCase node.
     * Prompts the user for a name. Updates model + UI + persistence.
     */
    private void createNewCase(TestNode caseNode) {
        if (caseNode == null || !(caseNode.getModelRef() instanceof TestCase)) return;
        String name = JOptionPane.showInputDialog(this, "Name des neuen TestCase:", "Neuer TestCase", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        TestCase oldCase = (TestCase) caseNode.getModelRef();
        TestNode suiteNode = (TestNode) caseNode.getParent();
        if (suiteNode == null || !(suiteNode.getModelRef() instanceof TestSuite)) return;

        TestSuite suite = (TestSuite) suiteNode.getModelRef();
        TestCase newCase = new TestCase(name.trim(), new ArrayList<>());

        List<TestCase> cases = suite.getTestCases();
        int idx = cases.indexOf(oldCase);
        if (idx < 0) idx = cases.size() - 1;
        cases.add(idx + 1, newCase);

        TestNode newCaseNode = new TestNode(newCase.getName(), newCase);
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.insertNodeInto(newCaseNode, suiteNode, idx + 1);
        testTree.expandPath(new TreePath(suiteNode.getPath()));

        TestRegistry.getInstance().save();
    }

    /**
     * Create a new step (When) directly after the given TestAction node.
     * Prompts for action name. Updates model + UI + persistence.
     */
    private void createNewStep(TestNode stepNode) {
        if (stepNode == null || !(stepNode.getModelRef() instanceof TestAction)) return;

        String actionName = JOptionPane.showInputDialog(this,
                "Aktion für neuen Step (z. B. click, navigate):",
                "Neuer Step",
                JOptionPane.PLAIN_MESSAGE);

        if (actionName == null || actionName.trim().isEmpty()) return;

        TestAction oldAction = (TestAction) stepNode.getModelRef();
        TestNode caseNode = (TestNode) stepNode.getParent();
        if (caseNode == null || !(caseNode.getModelRef() instanceof TestCase)) return;

        TestCase testCase = (TestCase) caseNode.getModelRef();

        TestAction newAction = new TestAction(actionName.trim());
        List<TestAction> actions = testCase.getWhen();

        int idx = actions.indexOf(oldAction);
        if (idx < 0) idx = actions.size() - 1;
        actions.add(idx + 1, newAction);

        TestNode newStepNode = new TestNode(renderActionLabel(newAction), newAction);
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.insertNodeInto(newStepNode, caseNode, idx + 1);
        testTree.expandPath(new TreePath(caseNode.getPath()));

        TestRegistry.getInstance().save();
    }

    /**
     * Create a new TestSuite directly after clicked (or append at root when null).
     * Prompts for name. Updates model + UI + persistence.
     */
    private void createNewSuiteAfter(TestNode clickedSuite) {
        String name = JOptionPane.showInputDialog(this, "Name der neuen Testsuite:", "Neue Testsuite", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        TestNode parentNode;
        int insertIndex;
        List<TestSuite> registryList = TestRegistry.getInstance().getAll();
        int registryInsertIndex;

        if (clickedSuite != null && clickedSuite.getModelRef() instanceof TestSuite) {
            TestSuite oldSuite = (TestSuite) clickedSuite.getModelRef();
            registryInsertIndex = registryList.indexOf(oldSuite);
            if (registryInsertIndex < 0) registryInsertIndex = registryList.size() - 1;
            registryInsertIndex++;

            parentNode = (TestNode) clickedSuite.getParent();
            if (parentNode == null) parentNode = (TestNode) testTree.getModel().getRoot();

            insertIndex = parentNode.getIndex(clickedSuite) + 1;
        } else {
            registryInsertIndex = registryList.size();
            parentNode = (TestNode) testTree.getModel().getRoot();
            insertIndex = parentNode.getChildCount();
        }

        TestSuite newSuite = new TestSuite(name.trim(), new ArrayList<>());
        registryList.add(registryInsertIndex, newSuite);

        TestNode newSuiteNode = new TestNode(newSuite.getName(), newSuite);
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.insertNodeInto(newSuiteNode, parentNode, insertIndex);
        testTree.expandPath(new TreePath(parentNode.getPath()));

        TestRegistry.getInstance().save();
    }

    private void createNewSuite() {
        String name = JOptionPane.showInputDialog(this, "Name der neuen Testsuite:", "Neue Testsuite", JOptionPane.PLAIN_MESSAGE);
        if (name != null && !name.trim().isEmpty()) {
            DefaultMutableTreeNode selected = getSelectedNodeOrRoot();
            DefaultMutableTreeNode newSuite = new DefaultMutableTreeNode(name.trim());
            DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
            model.insertNodeInto(newSuite, selected, selected.getChildCount());
        }
    }

    /**
     * Renames the selected node and updates the underlying model when applicable.
     */
    private void renameNode() {
        TestNode selected = getSelectedNode();
        if (selected == null || selected.getParent() == null) return; // Root nicht umbenennen

        String current = selected.toString();
        String name = JOptionPane.showInputDialog(this, "Neuer Name:", current);
        if (name == null || name.trim().isEmpty()) return;

        Object ref = selected.getModelRef();
        String trimmed = name.trim();

        if (ref instanceof TestSuite) {
            ((TestSuite) ref).setName(trimmed);
        } else if (ref instanceof TestCase) {
            ((TestCase) ref).setName(trimmed);
        } else {
            // Für Steps ggf. über Properties-Dialog
        }

        selected.setUserObject(trimmed);
        ((DefaultTreeModel) testTree.getModel()).nodeChanged(selected);
        TestRegistry.getInstance().save();
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

    /* ===================== Copy/Clone: Suite, Case, Step ===================== */

    /** Suite duplizieren: Cases tief (Actions neu), Insert direkt hinter geklickter Suite, Name unique "copy of …". */
    private void duplicateSuiteAfter(TestNode clickedSuite) {
        if (clickedSuite == null || !(clickedSuite.getModelRef() instanceof TestSuite)) return;

        TestSuite src = (TestSuite) clickedSuite.getModelRef();

        TestSuite copy = new TestSuite(uniqueSuiteName("copy of " + safeName(src.getName())), new ArrayList<>());
        for (TestCase c : src.getTestCases()) {
            copy.getTestCases().add(cloneCaseDeep(c, true));
        }

        List<TestSuite> suites = TestRegistry.getInstance().getAll();
        int insertIndex = ((DefaultMutableTreeNode) clickedSuite.getParent()).getIndex(clickedSuite) + 1;
        suites.add(insertIndex, copy);
        TestRegistry.getInstance().save();

        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        TestNode root = (TestNode) model.getRoot();
        TestNode newNode = new TestNode(copy.getName(), copy);
        model.insertNodeInto(newNode, root, Math.min(insertIndex, root.getChildCount()));
        selectNode(newNode);
    }

    /** Case duplizieren: Actions tief, Given/Then flach, Name "copy of …" unique in Suite. */
    private void duplicateCaseAfter(TestNode clickedCase) {
        if (clickedCase == null || !(clickedCase.getModelRef() instanceof TestCase)) return;

        DefaultMutableTreeNode suiteNode = (DefaultMutableTreeNode) clickedCase.getParent();
        Object suiteRef = (suiteNode instanceof TestNode) ? ((TestNode) suiteNode).getModelRef() : null;
        if (!(suiteRef instanceof TestSuite)) return;

        TestSuite suite = (TestSuite) suiteRef;
        TestCase src = (TestCase) clickedCase.getModelRef();

        TestCase copy = cloneCaseDeep(src, true);
        // Name in der Suite eindeutig machen:
        copy.setName(uniqueCaseName(suite, copy.getName()));

        List<TestCase> cases = suite.getTestCases();
        int insertIndex = suiteNode.getIndex(clickedCase) + 1;
        cases.add(insertIndex, copy);
        TestRegistry.getInstance().save();

        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        TestNode newNode = new TestNode(copy.getName(), copy);
        model.insertNodeInto(newNode, suiteNode, Math.min(insertIndex, suiteNode.getChildCount()));
        selectNode(newNode);
    }

    /** Step duplizieren: shallow copy (alle bekannten Felder); Insert direkt dahinter. */
    private void duplicateActionAfter(TestNode clickedAction) {
        if (clickedAction == null || !(clickedAction.getModelRef() instanceof TestAction)) return;

        DefaultMutableTreeNode caseNode = (DefaultMutableTreeNode) clickedAction.getParent();
        Object caseRef = (caseNode instanceof TestNode) ? ((TestNode) caseNode).getModelRef() : null;
        if (!(caseRef instanceof TestCase)) return;

        TestCase tc = (TestCase) caseRef;
        TestAction src = (TestAction) clickedAction.getModelRef();

        TestAction copy = cloneActionShallow(src, true);

        List<TestAction> steps = tc.getWhen();
        int insertIndex = caseNode.getIndex(clickedAction) + 1;
        steps.add(insertIndex, copy);
        TestRegistry.getInstance().save();

        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        TestNode newNode = new TestNode(renderActionLabel(copy), copy);
        model.insertNodeInto(newNode, caseNode, Math.min(insertIndex, caseNode.getChildCount()));
        selectNode(newNode);
    }

    /** Case „tief“ klonen (Actions neu), Given/Then flach per addAll; Name optional mit Prefix. */
    private TestCase cloneCaseDeep(TestCase src, boolean prefixedName) {
        String base = safeName(src.getName());
        String name = prefixedName ? "copy of " + base : base;

        // Actions tief kopieren
        List<TestAction> newWhen = new ArrayList<>();
        for (TestAction a : src.getWhen()) {
            newWhen.add(cloneActionShallow(a, false));
        }
        TestCase copy = new TestCase(name, newWhen);

        // Given/Then flach übernehmen
        copy.getGiven().addAll(src.getGiven());
        copy.getThen().addAll(src.getThen());
        return copy;
    }

    /** Step „shallow“ klonen – bekannte Felder übernehmen, optional Namen prefixen (falls vorhanden). */
    private TestAction cloneActionShallow(TestAction src, boolean tryPrefixName) {
        TestAction a = new TestAction();
        a.setType(src.getType());
        a.setAction(src.getAction());
        a.setValue(src.getValue());
        a.setUser(src.getUser());
        a.setTimeout(src.getTimeout());
        a.setSelectedSelector(src.getSelectedSelector());
        // optional: falls es einen getName()/setName() gibt
        if (tryPrefixName) {
            String n = getNameIfExists(src);
            if (n != null && !n.isEmpty()) {
                setNameIfExists(a, "copy of " + n);
            }
        }
        return a;
    }

    /* ===================== Helpers: naming, selection ===================== */

    private String safeName(String s) {
        return (s == null || s.trim().isEmpty()) ? "unnamed" : s.trim();
    }

    private String uniqueSuiteName(String base) {
        List<TestSuite> suites = TestRegistry.getInstance().getAll();
        Set<String> used = new HashSet<>();
        for (TestSuite s : suites) used.add(safeName(s.getName()));
        return makeUnique(base, used);
    }

    private String uniqueCaseName(TestSuite suite, String base) {
        Set<String> used = new HashSet<>();
        for (TestCase c : suite.getTestCases()) used.add(safeName(c.getName()));
        return makeUnique(base, used);
    }

    private String makeUnique(String base, Set<String> used) {
        String b = safeName(base);
        if (!used.contains(b)) return b;
        for (int i = 2; i < 10000; i++) {
            String cand = b + " (" + i + ")";
            if (!used.contains(cand)) return cand;
        }
        return b + " (copy)";
    }

    // Reflektions-Helper: optionales Name-Feld bei Actions
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

    // Komfort: Node selektieren & sichtbar machen
    private void selectNode(TestNode node) {
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        TreePath path = new TreePath(node.getPath());
        if (node.getParent() != null) {
            testTree.expandPath(new TreePath(((DefaultMutableTreeNode) node.getParent()).getPath()));
        }
        testTree.setSelectionPath(path);
        testTree.scrollPathToVisible(path);
        model.nodeChanged(node);
    }
}
