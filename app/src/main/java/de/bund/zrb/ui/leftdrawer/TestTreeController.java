package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.PreconditionSavedEvent;
import de.bund.zrb.model.*;
import de.bund.zrb.service.PreconditionFactory;
import de.bund.zrb.service.PreconditionRegistry;
import de.bund.zrb.service.TestRegistry;
import de.bund.zrb.ui.PropertiesDialog;
import de.bund.zrb.ui.TestNode;
import de.bund.zrb.ui.dialogs.ActionPickerDialog;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Controller für den Tests-Tree.
 * Nach dem Refactoring hängt er NICHT mehr an einer nackten List<TestSuite>,
 * sondern an einem RootNode-Datenmodell (mit UUIDs usw.).
 */
public class TestTreeController {

    private final JTree testTree;

    public TestTreeController(JTree testTree) {
        this.testTree = testTree;
    }

    // ========================= Build & Refresh =========================

    public static JTree buildTestTree() {
        // NEW: wir legen erstmal einen leeren RootNode-Knoten an
        TestNode rootNode = new TestNode("Testsuites", TestRegistry.getInstance().getRoot());
        JTree tree = new JTree(rootNode);
        tree.setCellRenderer(new TestTreeCellRenderer());
        return tree;
    }

    /**
     * Rebuild the visible JTree model from the underlying RootNode+Suites structure.
     * Call after load/save/add/remove...
     */
    public void refreshTestSuites(String nameIgnoredForNow) {
        RootNode rootModel = TestRegistry.getInstance().getRoot();
        TestNode rootNode = new TestNode("Testsuites", rootModel);

        for (TestSuite suite : rootModel.getTestSuites()) {
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

            rootNode.add(suiteNode);
        }

        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.setRoot(rootNode);
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
    // (unverändert außer den Stellen, wo wir jetzt über RootNode gehen)

    public void setupContextMenu() {
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

    public JPopupMenu buildContextMenu(TestNode clicked) {
        JPopupMenu menu = new JPopupMenu();

        // Root-Klick oder leerer Bereich -> neue Suite
        if (clicked == null || clicked.getModelRef() instanceof RootNode) {
            JMenuItem newSuite = new JMenuItem("Neue Testsuite");
            newSuite.addActionListener(evt -> createNewSuiteAfter(clicked));
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

        addCommonMenuItems(menu, clicked);
        return menu;
    }

    // ====== Precondition move bleibt wie gehabt (wir referenzieren Suite/Case Objekte direkt) ======

    public void moveCaseToPrecondition(TestNode clickedCase) {
        if (clickedCase == null || !(clickedCase.getModelRef() instanceof TestCase)) return;

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

        de.bund.zrb.model.Precondition pre = buildPreconditionFromCase(src, newName.trim());

        PreconditionRegistry.getInstance().addPrecondition(pre);
        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent(pre.getName()));

        // aus TestSuite entfernen
        TestNode suiteNode = (TestNode) clickedCase.getParent();
        Object suiteRef = (suiteNode != null ? suiteNode.getModelRef() : null);
        if (suiteRef instanceof TestSuite) {
            TestSuite suite = (TestSuite) suiteRef;
            suite.getTestCases().remove(src);
            TestRegistry.getInstance().save();

            DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
            model.removeNodeFromParent(clickedCase);
            model.nodeStructureChanged(suiteNode);
        } else {
            refreshTestSuites(null);
        }
    }

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

        de.bund.zrb.model.Precondition pre = buildPreconditionFromSuite(src, newName.trim());

        PreconditionRegistry.getInstance().addPrecondition(pre);
        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent(pre.getName()));

        // Suite aus Root entfernen
        RootNode rootModel = TestRegistry.getInstance().getRoot();
        rootModel.getTestSuites().remove(src);
        TestRegistry.getInstance().save();

        // UI aktualisieren
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) clickedSuite.getParent();
        if (parent != null) {
            model.removeNodeFromParent(clickedSuite);
            model.nodeStructureChanged(parent);
        } else {
            refreshTestSuites(null);
        }
    }

    private de.bund.zrb.model.Precondition buildPreconditionFromCase(TestCase src, String name) {
        de.bund.zrb.model.Precondition p = PreconditionFactory.newPrecondition(name);
        if (src.getGiven() != null) {
            p.getGiven().addAll(src.getGiven());
        }
        if (src.getWhen() != null) {
            for (TestAction a : src.getWhen()) {
                p.getActions().add(cloneActionShallow(a, false));
            }
        }
        return p;
    }

    private de.bund.zrb.model.Precondition buildPreconditionFromSuite(TestSuite src, String name) {
        de.bund.zrb.model.Precondition p = PreconditionFactory.newPrecondition(name);
        if (src.getGiven() != null) {
            p.getGiven().addAll(src.getGiven());
        }
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

    // ========================= Create/duplicate tests =========================

    /**
     * Neuer TestCase direkt nach dem gegebenen Case.
     */
    public void createNewCase(TestNode caseNode) {
        if (caseNode == null || !(caseNode.getModelRef() instanceof TestCase)) return;

        String name = JOptionPane.showInputDialog(testTree,
                "Name des neuen TestCase:", "Neuer TestCase", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        TestCase oldCase = (TestCase) caseNode.getModelRef();
        TestNode suiteNode = (TestNode) caseNode.getParent();
        if (suiteNode == null || !(suiteNode.getModelRef() instanceof TestSuite)) return;
        TestSuite suite = (TestSuite) suiteNode.getModelRef();

        TestCase newCase = new TestCase(name.trim(), new ArrayList<TestAction>());
        newCase.setParentId(suite.getId()); // NEW: parentId

        List<TestCase> cases = suite.getTestCases();
        int idx = cases.indexOf(oldCase);
        if (idx < 0) idx = cases.size() - 1;
        cases.add(idx + 1, newCase);

        // UI
        TestNode newCaseNode = new TestNode(newCase.getName(), newCase);
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.insertNodeInto(newCaseNode, suiteNode, idx + 1);
        testTree.expandPath(new TreePath(suiteNode.getPath()));

        TestRegistry.getInstance().save();
    }

    /**
     * Neuer When-Step direkt nach dem gegebenen Step.
     */
    public void createNewStep(TestNode stepNode) {
        if (stepNode == null || !(stepNode.getModelRef() instanceof TestAction)) return;

        Window owner = SwingUtilities.getWindowAncestor(testTree);
        ActionPickerDialog dlg = new ActionPickerDialog(owner, "Neuer Step", "click");
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;

        String actionName = dlg.getChosenAction();
        if (actionName.length() == 0) return;

        TestAction oldAction = (TestAction) stepNode.getModelRef();
        TestNode caseNode = (TestNode) stepNode.getParent();
        if (caseNode == null || !(caseNode.getModelRef() instanceof TestCase)) return;
        TestCase testCase = (TestCase) caseNode.getModelRef();

        TestAction newAction = new TestAction(actionName);
        newAction.setParentId(testCase.getId()); // NEW

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
     * Neue Testsuite direkt hinter der geklickten Suite
     * oder am Ende des Roots, falls null oder Root geklickt.
     */
    public void createNewSuiteAfter(TestNode clickedSuiteMaybe) {
        String name = JOptionPane.showInputDialog(testTree,
                "Name der neuen Testsuite:", "Neue Testsuite", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.trim().isEmpty()) return;

        RootNode rootModel = TestRegistry.getInstance().getRoot();
        List<TestSuite> registryList = rootModel.getTestSuites();

        TestNode parentNode;
        int insertIndex;
        int registryInsertIndex;

        if (clickedSuiteMaybe != null && clickedSuiteMaybe.getModelRef() instanceof TestSuite) {
            TestSuite oldSuite = (TestSuite) clickedSuiteMaybe.getModelRef();

            registryInsertIndex = registryList.indexOf(oldSuite);
            if (registryInsertIndex < 0) registryInsertIndex = registryList.size() - 1;
            registryInsertIndex++;

            parentNode = (TestNode) clickedSuiteMaybe.getParent(); // sollte RootNode-UI sein
            if (parentNode == null) parentNode = (TestNode) testTree.getModel().getRoot();

            insertIndex = parentNode.getIndex(clickedSuiteMaybe) + 1;
        } else {
            // Klick auf Root oder Leere Fläche
            registryInsertIndex = registryList.size();
            parentNode = (TestNode) testTree.getModel().getRoot();
            insertIndex = parentNode.getChildCount();
        }

        TestSuite newSuite = new TestSuite(name.trim(), new ArrayList<TestCase>());
        newSuite.setParentId(rootModel.getId()); // NEW

        registryList.add(registryInsertIndex, newSuite);

        TestNode newSuiteNode = new TestNode(newSuite.getName(), newSuite);
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.insertNodeInto(newSuiteNode, parentNode, insertIndex);
        testTree.expandPath(new TreePath(parentNode.getPath()));

        TestRegistry.getInstance().save();
    }

    // alter createNewSuite() kann theoretisch bleiben, aber wird von oben ersetzt;
    // du kannst ihn optional löschen oder belassen für Kompatibilität.

    public void renameNode() {
        TestNode selected = getSelectedNode();
        if (selected == null) return;

        // Root umbenennen? Nein.
        if (selected.getModelRef() instanceof RootNode) {
            return;
        }

        if (selected.getParent() == null) return;

        Object ref = selected.getModelRef();
        String trimmed;

        if (ref instanceof TestSuite) {
            String current = selected.toString();
            String name = JOptionPane.showInputDialog(testTree, "Neuer Name:", current);
            if (name == null || name.trim().isEmpty()) return;
            trimmed = name.trim();
            ((TestSuite) ref).setName(trimmed);
            selected.setUserObject(trimmed);
            ((DefaultTreeModel) testTree.getModel()).nodeChanged(selected);
            TestRegistry.getInstance().save();
            return;
        }

        if (ref instanceof TestCase) {
            String current = selected.toString();
            String name = JOptionPane.showInputDialog(testTree, "Neuer Name:", current);
            if (name == null || name.trim().isEmpty()) return;
            trimmed = name.trim();
            ((TestCase) ref).setName(trimmed);
            selected.setUserObject(trimmed);
            ((DefaultTreeModel) testTree.getModel()).nodeChanged(selected);
            TestRegistry.getInstance().save();
            return;
        }

        if (ref instanceof TestAction) {
            TestAction a = (TestAction) ref;
            Window owner = SwingUtilities.getWindowAncestor(testTree);
            ActionPickerDialog dlg = new ActionPickerDialog(owner, "Step umbenennen (Action)", a.getAction());
            dlg.setVisible(true);
            if (!dlg.isConfirmed()) return;

            String newAction = dlg.getChosenAction();
            if (newAction.length() == 0) return;

            a.setAction(newAction);
            selected.setUserObject(renderActionLabel(a));
            ((DefaultTreeModel) testTree.getModel()).nodeChanged(selected);
            TestRegistry.getInstance().save();
            return;
        }
    }

    public void deleteNode() {
        TestNode selected = (TestNode) testTree.getLastSelectedPathComponent();
        if (selected == null) return;

        // Root nie löschen
        if (selected.getModelRef() instanceof RootNode) {
            return;
        }

        if (selected.getParent() == null) return;

        Object refObj = selected.getModelRef();
        Object parentObj = ((TestNode) selected.getParent()).getModelRef();

        if (refObj instanceof TestSuite && parentObj instanceof RootNode) {
            RootNode rootModel = (RootNode) parentObj;
            rootModel.getTestSuites().remove(refObj);

        } else if (refObj instanceof TestCase && parentObj instanceof TestSuite) {
            ((TestSuite) parentObj).getTestCases().remove(refObj);

        } else if (parentObj instanceof TestCase) {
            TestCase testCase = (TestCase) parentObj;
            if (refObj instanceof TestAction) {
                testCase.getWhen().remove(refObj);
            } else if (refObj instanceof GivenCondition) {
                testCase.getGiven().remove(refObj);
            } else if (refObj instanceof ThenExpectation) {
                testCase.getThen().remove(refObj);
            }
        }

        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.removeNodeFromParent(selected);
        model.nodeStructureChanged((TestNode) selected.getParent());

        TestRegistry.getInstance().save();
    }

    public void openPropertiesDialog() {
        TestNode selected = getSelectedNode();
        if (selected != null && selected.getParent() != null) {
            // Optional: Root bekommt evtl. später einen eigenen Editor
            if (selected.getModelRef() instanceof RootNode) {
                return;
            }
            PropertiesDialog dialog = new PropertiesDialog(selected.toString());
            dialog.setVisible(true);
        }
    }

    // ========================= TestPlayerUi parts for tests tree =========================

    public TestNode getSelectedNode() {
        return (TestNode) testTree.getLastSelectedPathComponent();
    }

    public void updateNodeStatus(TestNode node, boolean passed) {
        node.setStatus(passed ? TestNode.Status.PASSED : TestNode.Status.FAILED);
        ((DefaultTreeModel) testTree.getModel()).nodeChanged(node);

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent instanceof TestNode) {
            updateSuiteStatus((TestNode) parent);
        }
    }

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

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) suite.getParent();
        if (parent instanceof TestNode) {
            updateSuiteStatus((TestNode) parent);
        }
    }

    public TestNode getRootNode() {
        return (TestNode) testTree.getModel().getRoot();
    }

    public DefaultMutableTreeNode getSelectedNodeOrRoot() {
        TestNode sel = getSelectedNode();
        return sel != null ? sel : (DefaultMutableTreeNode) testTree.getModel().getRoot();
    }

    /**
     * Für den Player: wir leiten jetzt über RootNode -> Suites.
     */
    public java.util.List<TestSuite> getSelectedSuites() {
        TreePath[] paths = testTree.getSelectionPaths();
        RootNode rootModel = TestRegistry.getInstance().getRoot();

        if (paths == null || paths.length == 0) {
            return rootModel.getTestSuites();
        }

        List<TestSuite> selectedSuites = new ArrayList<TestSuite>();
        for (TreePath p : paths) {
            Object n = p.getLastPathComponent();
            if (n instanceof TestNode) {
                Object ref = ((TestNode) n).getModelRef();
                if (ref instanceof TestSuite) {
                    selectedSuites.add((TestSuite) ref);
                }
            }
        }

        if (selectedSuites.isEmpty()) {
            return rootModel.getTestSuites();
        }
        return selectedSuites;
    }

    // ===================== Copy/Clone: Suite, Case, Step =====================

    public void duplicateSuiteAfter(TestNode clickedSuite) {
        if (clickedSuite == null || !(clickedSuite.getModelRef() instanceof TestSuite)) return;

        TestSuite src = (TestSuite) clickedSuite.getModelRef();

        TestSuite copy = new TestSuite(
                uniqueSuiteName("copy of " + safeName(src.getName())),
                new ArrayList<TestCase>()
        );
        copy.setParentId(src.getParentId()); // NEW: gleiche ParentId (Root bleibt gleich)

        for (TestCase c : src.getTestCases()) {
            TestCase clonedCase = cloneCaseDeep(c, true);
            clonedCase.setParentId(copy.getId()); // NEW
            copy.getTestCases().add(clonedCase);
        }

        RootNode rootModel = TestRegistry.getInstance().getRoot();
        List<TestSuite> suites = rootModel.getTestSuites();

        int insertIndex = ((DefaultMutableTreeNode) clickedSuite.getParent()).getIndex(clickedSuite) + 1;
        suites.add(insertIndex, copy);
        TestRegistry.getInstance().save();

        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        TestNode rootNode = (TestNode) model.getRoot();
        TestNode newNode = new TestNode(copy.getName(), copy);
        model.insertNodeInto(newNode, rootNode, Math.min(insertIndex, rootNode.getChildCount()));
        selectNode(newNode);
    }

    public void duplicateCaseAfter(TestNode clickedCase) {
        if (clickedCase == null || !(clickedCase.getModelRef() instanceof TestCase)) return;

        TestNode suiteNode = (TestNode) clickedCase.getParent();
        Object suiteRef = (suiteNode != null ? suiteNode.getModelRef() : null);
        if (!(suiteRef instanceof TestSuite)) return;

        TestSuite suite = (TestSuite) suiteRef;
        TestCase src = (TestCase) clickedCase.getModelRef();

        TestCase copy = cloneCaseDeep(src, true);
        // Parent setzen
        copy.setParentId(suite.getId());

        // Namen in Suite eindeutig machen
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

    public void duplicateActionAfter(TestNode clickedAction) {
        if (clickedAction == null || !(clickedAction.getModelRef() instanceof TestAction)) return;

        TestNode caseNode = (TestNode) clickedAction.getParent();
        Object caseRef = (caseNode != null ? caseNode.getModelRef() : null);
        if (!(caseRef instanceof TestCase)) return;

        TestCase tc = (TestCase) caseRef;
        TestAction src = (TestAction) clickedAction.getModelRef();

        TestAction copy = cloneActionShallow(src, true);
        copy.setParentId(tc.getId()); // NEW

        List<TestAction> steps = tc.getWhen();
        int insertIndex = caseNode.getIndex(clickedAction) + 1;
        steps.add(insertIndex, copy);
        TestRegistry.getInstance().save();

        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        TestNode newNode = new TestNode(renderActionLabel(copy), copy);
        model.insertNodeInto(newNode, caseNode, Math.min(insertIndex, caseNode.getChildCount()));
        selectNode(newNode);
    }

    public TestCase cloneCaseDeep(TestCase src, boolean prefixedName) {
        String base = safeName(src.getName());
        String name = prefixedName ? "copy of " + base : base;

        // Actions tief kopieren
        List<TestAction> newWhen = new ArrayList<TestAction>();
        for (TestAction a : src.getWhen()) {
            TestAction clonedAction = cloneActionShallow(a, false);
            // parentId setzen wir NICHT hier, sondern da wo wir den Case ins Parent hängen
            newWhen.add(clonedAction);
        }
        TestCase copy = new TestCase(name, newWhen);

        // parentId setzt der Caller
        // Given / Then flach übernehmen wie gehabt
        copy.getGiven().addAll(src.getGiven());
        copy.getThen().addAll(src.getThen());

        return copy;
    }

    public TestAction cloneActionShallow(TestAction src, boolean tryPrefixName) {
        TestAction a = new TestAction();
        a.setType(src.getType());
        a.setAction(src.getAction());
        a.setValue(src.getValue());
        a.setUser(src.getUser());
        a.setTimeout(src.getTimeout());
        a.setSelectedSelector(src.getSelectedSelector());
        a.setLocatorType(src.getLocatorType());

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
        RootNode rootModel = TestRegistry.getInstance().getRoot();
        Set<String> used = new HashSet<String>();
        for (TestSuite s : rootModel.getTestSuites()) {
            used.add(safeName(s.getName()));
        }
        return makeUnique(base, used);
    }

    public String uniqueCaseName(TestSuite suite, String base) {
        Set<String> used = new HashSet<String>();
        for (TestCase c : suite.getTestCases()) {
            used.add(safeName(c.getName()));
        }
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
        } catch (Exception ignore) {
            // Action hat evtl. keinen Namen
        }
    }

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
