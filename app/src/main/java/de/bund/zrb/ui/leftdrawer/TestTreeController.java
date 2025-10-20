package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.PreconditionSavedEvent;
import de.bund.zrb.model.*;
import de.bund.zrb.service.PreconditionFactory;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.PropertiesDialog;
import de.bund.zrb.ui.TestNode;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.*;

/**
 * Encapsulate all behavior for the "Tests" tree to keep LeftDrawer slim.
 * Methods are copied from LeftDrawer unchanged in content and comments.
 */
public class TestTreeController {

    private final JTree testTree;

    public TestTreeController(JTree testTree) {
        this.testTree = testTree;
    }

    // ========================= Build & Refresh =========================

    public static JTree buildTestTree() {
        TestNode root = new TestNode("Testsuites");
        JTree tree = new JTree(root);
        tree.setCellRenderer(new TestTreeCellRenderer());
        return tree;
    }

    public void refreshTestSuites(String name) {
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

    public String renderActionLabel(TestAction action) {
        String label = action.getAction();
        if (action.getValue() != null && !action.getValue().isEmpty()) {
            label += " [" + action.getValue() + "]";
        } else if (action.getSelectedSelector() != null && !action.getSelectedSelector().isEmpty()) {
            label += " [" + action.getSelectedSelector() + "]";
        }
        return label;
    }

    // ========================= Context menu (tests) =========================

    /**
     * Set up a dynamic context menu for the test tree. Depending on the clicked node,
     * offer to create a new TestSuite, TestCase or Step (When) – and offer "Kopie erstellen".
     * Rename, delete and properties actions are preserved. New/copied elements are inserted
     * directly AFTER the clicked element within its parent (or at root if empty area).
     */
    public void setupContextMenu() {
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
    public JPopupMenu buildContextMenu(TestNode clicked) {
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

            JMenuItem moveSuiteToPrecond = new JMenuItem("In Precondition verschieben…");
            moveSuiteToPrecond.addActionListener(evt -> moveSuiteToPrecondition(clicked));
            menu.add(moveSuiteToPrecond);

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

            JMenuItem moveCaseToPrecond = new JMenuItem("In Precondition verschieben…");
            moveCaseToPrecond.addActionListener(evt -> moveCaseToPrecondition(clicked));
            menu.add(moveCaseToPrecond);

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

    /** Move a single TestCase into a new Precondition and remove it from the suite. */
    public void moveCaseToPrecondition(TestNode clickedCase) {
        if (clickedCase == null || !(clickedCase.getModelRef() instanceof TestCase)) return;

        // Confirm with the user
        TestCase src = (TestCase) clickedCase.getModelRef();
        String defaultName = safeName(src.getName());
        int confirm = JOptionPane.showConfirmDialog(
                testTree,
                "Soll der TestCase \"" + defaultName + "\" wirklich in eine Precondition verschoben werden?\n" +
                        "Der TestCase wird aus der Testsuite entfernt.",
                "In Precondition verschieben",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        String newName = JOptionPane.showInputDialog(
                testTree, "Name der Precondition:", defaultName
        );
        if (newName == null || newName.trim().isEmpty()) return;

        // Build precondition from case (Given + When; Then ignored)
        de.bund.zrb.model.Precondition pre = buildPreconditionFromCase(src, newName.trim());

        // Persist precondition (add or update) and fire event
        PreconditionRegistry.getInstance().addPrecondition(pre);
        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent(pre.getName()));

        // Remove the case from its suite and persist tests
        DefaultMutableTreeNode suiteNode = (DefaultMutableTreeNode) clickedCase.getParent();
        Object suiteRef = (suiteNode instanceof TestNode) ? ((TestNode) suiteNode).getModelRef() : null;
        if (suiteRef instanceof TestSuite) {
            TestSuite suite = (TestSuite) suiteRef;
            suite.getTestCases().remove(src);
            TestRegistry.getInstance().save();

            // Update UI
            DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
            model.removeNodeFromParent(clickedCase);
            model.nodeStructureChanged(suiteNode);
        } else {
            // Fallback: full refresh
            refreshTestSuites(null);
        }
    }

    /** Move an entire TestSuite into a new Precondition and remove the suite. */
    public void moveSuiteToPrecondition(TestNode clickedSuite) {
        if (clickedSuite == null || !(clickedSuite.getModelRef() instanceof TestSuite)) return;

        TestSuite src = (TestSuite) clickedSuite.getModelRef();
        String defaultName = safeName(src.getName());
        int confirm = JOptionPane.showConfirmDialog(
                testTree,
                "Soll die Testsuite \"" + defaultName + "\" wirklich in eine Precondition verschoben werden?\n" +
                        "Die Testsuite wird aus dem Testbaum entfernt.",
                "In Precondition verschieben",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm != JOptionPane.YES_OPTION) return;

        String newName = JOptionPane.showInputDialog(
                testTree, "Name der Precondition:", defaultName
        );
        if (newName == null || newName.trim().isEmpty()) return;

        // Build precondition from suite (Given + all When of its cases; Then ignored)
        de.bund.zrb.model.Precondition pre = buildPreconditionFromSuite(src, newName.trim());

        // Persist precondition and fire event
        PreconditionRegistry.getInstance().addPrecondition(pre);
        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent(pre.getName()));

        // Remove suite from registry and persist tests
        List<TestSuite> suites = TestRegistry.getInstance().getAll();
        suites.remove(src);
        TestRegistry.getInstance().save();

        // Update UI
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) clickedSuite.getParent();
        if (parent != null) {
            model.removeNodeFromParent(clickedSuite);
            model.nodeStructureChanged(parent);
        } else {
            refreshTestSuites(null);
        }
    }

    /** Build a Precondition from a TestCase: copy Given + When; ignore Then. */
    private de.bund.zrb.model.Precondition buildPreconditionFromCase(TestCase src, String name) {
        de.bund.zrb.model.Precondition p = PreconditionFactory.newPrecondition(name);
        // Reuse Given list (shallow) as done elsewhere; optional deep copy later
        if (src.getGiven() != null) {
            p.getGiven().addAll(src.getGiven());
        }
        // Deep copy actions to make the precondition independent
        if (src.getWhen() != null) {
            for (TestAction a : src.getWhen()) {
                p.getActions().add(cloneActionShallow(a, false));
            }
        }
        return p;
    }

    /** Build a Precondition from a TestSuite: suite-Givens + all case When steps in order. */
    private de.bund.zrb.model.Precondition buildPreconditionFromSuite(TestSuite src, String name) {
        de.bund.zrb.model.Precondition p = PreconditionFactory.newPrecondition(name);
        // Suite-level givens
        if (src.getGiven() != null) {
            p.getGiven().addAll(src.getGiven());
        }
        // Flatten all When actions from cases (in order)
        if (src.getTestCases() != null) {
            for (TestCase c : src.getTestCases()) {
                if (c.getWhen() == null) continue;
                for (TestAction a : c.getWhen()) {
                    p.getActions().add(cloneActionShallow(a, false));
                }
            }
        }
        return p;
    }

    public void addCommonMenuItems(JPopupMenu menu, TestNode onNode) {
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

    // ========================= Create/duplicate tests (unchanged) =========================

    /**
     * Create a new TestCase directly after the given TestCase node.
     * Prompts the user for a name. Updates model + UI + persistence.
     */
    public void createNewCase(TestNode caseNode) {
        if (caseNode == null || !(caseNode.getModelRef() instanceof TestCase)) return;
        String name = JOptionPane.showInputDialog(testTree, "Name des neuen TestCase:", "Neuer TestCase", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        TestCase oldCase = (TestCase) caseNode.getModelRef();
        TestNode suiteNode = (TestNode) caseNode.getParent();
        if (suiteNode == null || !(suiteNode.getModelRef() instanceof TestSuite)) return;

        TestSuite suite = (TestSuite) suiteNode.getModelRef();
        TestCase newCase = new TestCase(name.trim(), new ArrayList<TestAction>());

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
    public void createNewStep(TestNode stepNode) {
        if (stepNode == null || !(stepNode.getModelRef() instanceof TestAction)) return;

        String actionName = JOptionPane.showInputDialog(testTree,
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
    public void createNewSuiteAfter(TestNode clickedSuite) {
        String name = JOptionPane.showInputDialog(testTree, "Name der neuen Testsuite:", "Neue Testsuite", JOptionPane.PLAIN_MESSAGE);
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

        TestSuite newSuite = new TestSuite(name.trim(), new ArrayList<TestCase>());
        registryList.add(registryInsertIndex, newSuite);

        TestNode newSuiteNode = new TestNode(newSuite.getName(), newSuite);
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.insertNodeInto(newSuiteNode, parentNode, insertIndex);
        testTree.expandPath(new TreePath(parentNode.getPath()));

        TestRegistry.getInstance().save();
    }

    public void createNewSuite() {
        String name = JOptionPane.showInputDialog(testTree, "Name der neuen Testsuite:", "Neue Testsuite", JOptionPane.PLAIN_MESSAGE);
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
    public void renameNode() {
        TestNode selected = getSelectedNode();
        if (selected == null || selected.getParent() == null) return; // Root nicht umbenennen

        String current = selected.toString();
        String name = JOptionPane.showInputDialog(testTree, "Neuer Name:", current);
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

    public void deleteNode() {
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

    public void openPropertiesDialog() {
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null && selected.getParent() != null) {
            PropertiesDialog dialog = new PropertiesDialog(selected.toString());
            dialog.setVisible(true);
        }
    }

    // ========================= TestPlayerUi parts for tests tree =========================

    public TestNode getSelectedNode() {
        return (TestNode) testTree.getLastSelectedPathComponent();
    }

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

    public TestNode getRootNode() {
        return (TestNode) testTree.getModel().getRoot();
    }

    public DefaultMutableTreeNode getSelectedNodeOrRoot() {
        DefaultMutableTreeNode selected = getSelectedNode();
        return selected != null ? selected : (DefaultMutableTreeNode) testTree.getModel().getRoot();
    }

    public java.util.List<TestSuite> getSelectedSuites() {
        TreePath[] paths = testTree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return TestRegistry.getInstance().getAll();
        }

        java.util.List<TestSuite> selected = new java.util.ArrayList<TestSuite>();
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

    // ===================== Copy/Clone: Suite, Case, Step =====================

    /** Suite duplizieren: Cases tief (Actions neu), Insert direkt hinter geklickter Suite, Name unique "copy of …". */
    public void duplicateSuiteAfter(TestNode clickedSuite) {
        if (clickedSuite == null || !(clickedSuite.getModelRef() instanceof TestSuite)) return;

        TestSuite src = (TestSuite) clickedSuite.getModelRef();

        TestSuite copy = new TestSuite(uniqueSuiteName("copy of " + safeName(src.getName())), new ArrayList<TestCase>());
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
    public void duplicateCaseAfter(TestNode clickedCase) {
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
    public void duplicateActionAfter(TestNode clickedAction) {
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
    public TestCase cloneCaseDeep(TestCase src, boolean prefixedName) {
        String base = safeName(src.getName());
        String name = prefixedName ? "copy of " + base : base;

        // Actions tief kopieren
        List<TestAction> newWhen = new ArrayList<TestAction>();
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
    public TestAction cloneActionShallow(TestAction src, boolean tryPrefixName) {
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

    // ===================== Helpers: naming, selection =====================

    public String safeName(String s) {
        return (s == null || s.trim().isEmpty()) ? "unnamed" : s.trim();
    }

    public String uniqueSuiteName(String base) {
        List<TestSuite> suites = TestRegistry.getInstance().getAll();
        Set<String> used = new HashSet<String>();
        for (TestSuite s : suites) used.add(safeName(s.getName()));
        return makeUnique(base, used);
    }

    public String uniqueCaseName(TestSuite suite, String base) {
        Set<String> used = new HashSet<String>();
        for (TestCase c : suite.getTestCases()) used.add(safeName(c.getName()));
        return makeUnique(base, used);
    }

    public String makeUnique(String base, Set<String> used) {
        String b = safeName(base);
        if (!used.contains(b)) return b;
        for (int i = 2; i < 10000; i++) {
            String cand = b + " (" + i + ")";
            if (!used.contains(cand)) return cand;
        }
        return b + " (copy)";
    }

    // Reflektions-Helper: optionales Name-Feld bei Actions
    public String getNameIfExists(Object bean) {
        try {
            java.lang.reflect.Method m = bean.getClass().getMethod("getName");
            Object v = m.invoke(bean);
            return (v != null) ? String.valueOf(v) : null;
        } catch (Exception ignore) { return null; }
    }
    public void setNameIfExists(Object bean, String name) {
        try {
            java.lang.reflect.Method m = bean.getClass().getMethod("setName", String.class);
            m.invoke(bean, name);
        } catch (Exception ignore) { /* kein Name vorhanden */ }
    }

    // Komfort: Node selektieren & sichtbar machen
    public void selectNode(TestNode node) {
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
