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
import java.util.UUID;

/**
 * Controller für den Testbaum (LeftDrawer).
 *
 * WICHTIG: Ab jetzt basiert der Baum nicht mehr auf einer losen List<TestSuite>,
 * sondern auf dem "richtigen" Datenmodell:
 *
 * RootNode
 *   -> List<TestSuite>
 *        -> List<TestCase>
 *             -> List<TestAction>
 *
 * TestRegistry hält genau EIN RootNode.
 *
 * Dieser Controller rendert diesen RootNode in Swing (TestNode-Knoten)
 * und nimmt Änderungen am Modell vor (create/rename/delete/duplicate),
 * ruft danach TestRegistry.save(), und spiegelt die Änderung zurück in den TreeModel.
 */
public class TestTreeController {

    private final JTree testTree;

    public TestTreeController(JTree testTree) {
        this.testTree = testTree;
    }

    // ========================= Build & Refresh =========================

    /**
     * Erzeugt einen neuen JTree mit unserem RootNode aus der Registry.
     * (Wird z.B. in LeftDrawer-Konstruktor benutzt.)
     */
    public static JTree buildTestTree() {
        TestRegistry reg = TestRegistry.getInstance();
        RootNode rootModel = reg.getRoot();

        TestNode rootUi = buildUiTreeFromModel(rootModel);

        JTree tree = new JTree(rootUi);
        tree.setCellRenderer(new TestTreeCellRenderer());
        return tree;
    }

    /**
     * Baut den kompletten UI-Baum NEU aus dem Registry-Modell
     * und hängt ihn in das existierende JTree rein.
     */
    public void refreshTreeFromRegistry() {
        TestRegistry reg = TestRegistry.getInstance();
        RootNode rootModel = reg.getRoot();

        TestNode newRootUi = buildUiTreeFromModel(rootModel);

        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.setRoot(newRootUi);
        model.reload();
    }

    /**
     * Hilfsfunktion: nimmt RootNode/TestSuite/TestCase/TestAction
     * und erzeugt die Swing-Nodes (TestNode) mit korrekten Labels.
     */
    private static TestNode buildUiTreeFromModel(RootNode rootModel) {
        // Root-Knoten im UI zeigt z.B. "Testsuites", hängt aber das RootNode-Objekt an.
        String rootLabel = "Testsuites";
        TestNode rootUi = new TestNode(rootLabel, rootModel);

        if (rootModel == null || rootModel.getTestSuites() == null) {
            return rootUi;
        }

        for (TestSuite suite : rootModel.getTestSuites()) {
            TestNode suiteNode = new TestNode(suite.getName(), suite);

            if (suite.getTestCases() != null) {
                for (TestCase testCase : suite.getTestCases()) {
                    TestNode caseNode = new TestNode(testCase.getName(), testCase);

                    if (testCase.getWhen() != null) {
                        for (TestAction action : testCase.getWhen()) {
                            String label = renderActionLabel(action);
                            TestNode stepNode = new TestNode(label, action);
                            caseNode.add(stepNode);
                        }
                    }

                    suiteNode.add(caseNode);
                }
            }

            rootUi.add(suiteNode);
        }

        return rootUi;
    }

    public static String renderActionLabel(TestAction action) {
        String label = action.getAction();
        if (action.getValue() != null && !action.getValue().isEmpty()) {
            label += " [" + action.getValue() + "]";
        } else if (action.getSelectedSelector() != null && !action.getSelectedSelector().isEmpty()) {
            label += " [" + action.getSelectedSelector() + "]";
        }
        return label;
    }

    // ========================= Context menu (tests) =========================

    public void setupContextMenu() {
        // kein statisches PopupMenu direkt am JTree setzen (wir bauen dynamisch)
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
                        testTree.setSelectionPath(path); // damit rename/delete wissen, worum's geht
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
     * Bilde das Kontextmenü je nach Node-Typ.
     */
    public JPopupMenu buildContextMenu(TestNode clicked) {
        JPopupMenu menu = new JPopupMenu();

        if (clicked == null) {
            // Klick ins Leere -> neue Suite anlegen
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

    // ========================= Aktionen: Suite/Case/Step verschieben als Precondition =========================

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

        String newName = JOptionPane.showInputDialog(testTree, "Name der Precondition:", defaultName);
        if (newName == null || newName.trim().isEmpty()) return;

        // Precondition bauen
        de.bund.zrb.model.Precondition pre = buildPreconditionFromCase(src, newName.trim());
        PreconditionRegistry.getInstance().addPrecondition(pre);
        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent(pre.getName()));

        // Aus Modell entfernen
        DefaultMutableTreeNode suiteNode = (DefaultMutableTreeNode) clickedCase.getParent();
        Object suiteRef = (suiteNode instanceof TestNode) ? ((TestNode) suiteNode).getModelRef() : null;
        if (suiteRef instanceof TestSuite) {
            TestSuite suite = (TestSuite) suiteRef;
            suite.getTestCases().remove(src);
            TestRegistry.getInstance().save();

            // UI updaten
            DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
            model.removeNodeFromParent(clickedCase);
            model.nodeStructureChanged(suiteNode);
        } else {
            refreshTreeFromRegistry();
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

        String newName = JOptionPane.showInputDialog(testTree, "Name der Precondition:", defaultName);
        if (newName == null || newName.trim().isEmpty()) return;

        de.bund.zrb.model.Precondition pre = buildPreconditionFromSuite(src, newName.trim());
        PreconditionRegistry.getInstance().addPrecondition(pre);
        PreconditionRegistry.getInstance().save();
        ApplicationEventBus.getInstance().publish(new PreconditionSavedEvent(pre.getName()));

        // Suite aus Registry löschen
        TestRegistry reg = TestRegistry.getInstance();
        reg.getRoot().getTestSuites().remove(src);
        reg.save();

        // UI aktualisieren
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) clickedSuite.getParent();
        if (parent != null) {
            model.removeNodeFromParent(clickedSuite);
            model.nodeStructureChanged(parent);
        } else {
            refreshTreeFromRegistry();
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

    // ========================= Create/duplicate =========================

    /**
     * Neuer TestCase NACH dem gegebenen Case in derselben Suite.
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
        // IDs setzen
        newCase.setId(UUID.randomUUID().toString());
        newCase.setParentId(suite.getId());

        List<TestCase> cases = suite.getTestCases();
        int idx = cases.indexOf(oldCase);
        if (idx < 0) idx = cases.size() - 1;
        cases.add(idx + 1, newCase);

        // UI einfügen
        TestNode newCaseNode = new TestNode(newCase.getName(), newCase);
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.insertNodeInto(newCaseNode, suiteNode, idx + 1);
        testTree.expandPath(new TreePath(suiteNode.getPath()));

        TestRegistry.getInstance().save();
    }

    /**
     * Neuer Schritt nach gegebenem Schritt im gleichen Case.
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
        // IDs setzen
        newAction.setId(UUID.randomUUID().toString());
        newAction.setParentId(testCase.getId());
        newAction.setType(TestAction.ActionType.WHEN);

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

    /** Hilfsfunktion für UUIDs, damit's lesbarer ist. */
    private String newUuid() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Neue Suite hinter (oder am Ende von Root) einfügen.
     * Modell ändern, speichern, dann Tree neu rendern.
     */
    public void createNewSuiteAfter(TestNode clickedSuiteNode) {
        // 1. Name erfragen
        String name = JOptionPane.showInputDialog(
                testTree,
                "Name der neuen Testsuite:",
                "Neue Testsuite",
                JOptionPane.PLAIN_MESSAGE
        );
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        String trimmedName = name.trim();

        // 2. Modell holen
        TestRegistry reg = TestRegistry.getInstance();
        RootNode rootModel = reg.getRoot();
        java.util.List<TestSuite> suites = rootModel.getTestSuites();
        if (suites == null) {
            // sollte durch repair nie null sein, aber wir sichern uns doppelt
            suites = new java.util.ArrayList<>();
            rootModel.setTestSuites(suites);
        }

        // 3. Neue Suite aufbauen (UUID, parentId, etc.)
        TestSuite newSuite = new TestSuite(trimmedName, new java.util.ArrayList<TestCase>());
        newSuite.setId(newUuid());
        newSuite.setParentId(rootModel.getId());
        newSuite.setDescription(""); // erstmal leer

        // 4. Einfüge-Index bestimmen
        int insertIndex = suites.size(); // default: ans Ende
        if (clickedSuiteNode != null && clickedSuiteNode.getModelRef() instanceof TestSuite) {
            TestSuite clickedSuite = (TestSuite) clickedSuiteNode.getModelRef();
            int idx = suites.indexOf(clickedSuite);
            if (idx >= 0) {
                insertIndex = idx + 1;
            }
        }

        // 5. Suite einhängen
        suites.add(insertIndex, newSuite);

        // 6. Speichern
        reg.save();

        // 7. UI komplett neu rendern
        refreshTestTree();

        // 8. (Optional) neue Suite direkt selektieren
        selectNodeByModelRef(newSuite);
    }

    /**
     * Komfort: versucht nach refreshTestTree() den Knoten zu selektieren,
     * der dieses Modellobjekt trägt.
     */
    private void selectNodeByModelRef(Object targetRef) {
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        Object rootObj = model.getRoot();
        if (!(rootObj instanceof TestNode)) return;
        TestNode rootNode = (TestNode) rootObj;

        TestNode match = findNodeByRefRecursive(rootNode, targetRef);
        if (match != null) {
            selectNode(match);
        }
    }

    private TestNode findNodeByRefRecursive(TestNode current, Object target) {
        if (current.getModelRef() == target) {
            return current;
        }
        for (int i = 0; i < current.getChildCount(); i++) {
            Object child = current.getChildAt(i);
            if (child instanceof TestNode) {
                TestNode tn = (TestNode) child;
                TestNode found = findNodeByRefRecursive(tn, target);
                if (found != null) return found;
            }
        }
        return null;
    }

    // ========================= Rename / Delete =========================

    public void renameNode() {
        TestNode selected = getSelectedNode();
        if (selected == null || selected.getParent() == null) return; // Root nicht umbenennen

        Object ref = selected.getModelRef();

        if (ref instanceof TestSuite) {
            TestSuite suite = (TestSuite) ref;
            String current = selected.toString();
            String name = JOptionPane.showInputDialog(testTree, "Neuer Name:", current);
            if (name == null || name.trim().isEmpty()) return;
            String trimmed = name.trim();
            suite.setName(trimmed);
            selected.setUserObject(trimmed);
            ((DefaultTreeModel) testTree.getModel()).nodeChanged(selected);
            TestRegistry.getInstance().save();
            return;
        }

        if (ref instanceof TestCase) {
            TestCase tc = (TestCase) ref;
            String current = selected.toString();
            String name = JOptionPane.showInputDialog(testTree, "Neuer Name:", current);
            if (name == null || name.trim().isEmpty()) return;
            String trimmed = name.trim();
            tc.setName(trimmed);
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
        if (selected == null || selected.getParent() == null) return;

        Object currentObj = selected.getModelRef();
        Object parentObj = ((TestNode) selected.getParent()).getModelRef();

        TestRegistry reg = TestRegistry.getInstance();

        if (currentObj instanceof TestSuite) {
            // direkt aus root entfernen
            reg.getRoot().getTestSuites().remove(currentObj);

        } else if (currentObj instanceof TestCase && parentObj instanceof TestSuite) {
            ((TestSuite) parentObj).getTestCases().remove(currentObj);

        } else if (parentObj instanceof TestCase) {
            TestCase testCase = (TestCase) parentObj;

            if (currentObj instanceof TestAction) {
                testCase.getWhen().remove(currentObj);

            } else if (currentObj instanceof GivenCondition) {
                testCase.getGiven().remove(currentObj);

            } else if (currentObj instanceof ThenExpectation) {
                testCase.getThen().remove(currentObj);
            }
        }

        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.removeNodeFromParent(selected);
        model.nodeStructureChanged((TestNode) selected.getParent());

        reg.save();
    }

    // ========================= Misc / UI Helpers =========================

    public void openPropertiesDialog() {
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null && selected.getParent() != null) {
            PropertiesDialog dialog = new PropertiesDialog(selected.toString());
            dialog.setVisible(true);
        }
    }

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
        DefaultMutableTreeNode selected = getSelectedNode();
        return selected != null ? selected : (DefaultMutableTreeNode) testTree.getModel().getRoot();
    }

    /**
     * Liefert aktuell selektierte Suites, oder alle Suites falls keine selektiert.
     * Wird noch für den TestPlayer genutzt.
     */
    public List<TestSuite> getSelectedSuites() {
        TreePath[] paths = testTree.getSelectionPaths();
        if (paths == null || paths.length == 0) {
            return TestRegistry.getInstance().getRoot().getTestSuites();
        }

        List<TestSuite> selected = new ArrayList<TestSuite>();
        for (TreePath path : paths) {
            Object node = path.getLastPathComponent();
            if (node instanceof TestNode) {
                Object ref = ((TestNode) node).getModelRef();
                if (ref instanceof TestSuite) {
                    selected.add((TestSuite) ref);
                }
            }
        }

        if (selected.isEmpty()) {
            return TestRegistry.getInstance().getRoot().getTestSuites();
        }
        return selected;
    }

    public TestCase cloneCaseDeep(TestCase src, boolean prefixedName) {
        String base = safeName(src.getName());
        String name = prefixedName ? "copy of " + base : base;

        // Actions tief klonen
        List<TestAction> newWhen = new ArrayList<TestAction>();
        if (src.getWhen() != null) {
            for (TestAction a : src.getWhen()) {
                TestAction clone = cloneActionShallow(a, false);
                newWhen.add(clone);
            }
        }

        TestCase copy = new TestCase(name, newWhen);

        // IDs für den Clone: wir setzen erst später parentId korrekt, wenn wir ihn einer Suite hinzufügen.
        copy.setId(UUID.randomUUID().toString());

        if (src.getGiven() != null) {
            copy.getGiven().addAll(src.getGiven());
        }
        if (src.getThen() != null) {
            copy.getThen().addAll(src.getThen());
        }

        return copy;
    }

    public TestAction cloneActionShallow(TestAction src, boolean tryPrefixName) {
        TestAction a = new TestAction();
        a.setId(UUID.randomUUID().toString());

        a.setType(src.getType() != null ? src.getType() : TestAction.ActionType.WHEN);
        a.setAction(src.getAction());
        a.setValue(src.getValue());
        a.setUser(src.getUser());
        a.setTimeout(src.getTimeout());
        a.setSelectedSelector(src.getSelectedSelector());
        a.setLocatorType(src.getLocatorType());
        a.setLocators(new LinkedHashMap<String, String>(src.getLocators()));
        a.setExtractedValues(new LinkedHashMap<String, String>(src.getExtractedValues()));
        a.setExtractedAttributes(new LinkedHashMap<String, String>(src.getExtractedAttributes()));
        a.setExtractedTestIds(new LinkedHashMap<String, String>(src.getExtractedTestIds()));
        a.setExtractedAriaRoles(new LinkedHashMap<String, String>(src.getExtractedAriaRoles()));
        a.setText(src.getText());
        a.setRole(src.getRole());
        a.setLabel(src.getLabel());
        a.setRaw(src.getRaw());

        // Falls du jemals Namen an Actions hängen willst:
        if (tryPrefixName) {
            String n = getNameIfExists(src);
            if (n != null && !n.isEmpty()) {
                setNameIfExists(a, "copy of " + n);
            }
        }
        return a;
    }

    public String safeName(String s) {
        return (s == null || s.trim().isEmpty()) ? "unnamed" : s.trim();
    }

    public String uniqueSuiteName(String base) {
        List<TestSuite> suites = TestRegistry.getInstance().getRoot().getTestSuites();
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

    // Reflektions-Helfer: optionales getName/setName bei Actions
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

    /** Suite duplizieren: Cases tief (Actions neu), Insert direkt hinter geklickter Suite, Name unique "copy of …". */
    public void duplicateSuiteAfter(TestNode clickedSuite) {
        if (clickedSuite == null || !(clickedSuite.getModelRef() instanceof TestSuite)) return;

        TestRegistry reg = TestRegistry.getInstance();
        RootNode rootModel = reg.getRoot();

        TestSuite src = (TestSuite) clickedSuite.getModelRef();

        // Basisname vorbereiten
        String baseName = "copy of " + safeName(src.getName());
        String uniqueName = uniqueSuiteName(baseName);

        // Neue Suite anlegen
        TestSuite copy = new TestSuite(uniqueName, new ArrayList<TestCase>());
        copy.setId(UUID.randomUUID().toString());
        copy.setParentId(rootModel.getId());
        copy.setDescription(src.getDescription()); // Beschreibung mitnehmen

        // Alle Cases tief klonen
        for (TestCase c : src.getTestCases()) {
            TestCase clonedCase = cloneCaseDeep(c, false /* wir geben Namen unten unique */);

            // jetzt parentId für den geklonten Case setzen
            clonedCase.setParentId(copy.getId());

            // Actions im geklonten Case müssen auch die parentId kennen:
            if (clonedCase.getWhen() != null) {
                for (TestAction a : clonedCase.getWhen()) {
                    a.setParentId(clonedCase.getId());
                    if (a.getType() == null) {
                        a.setType(TestAction.ActionType.WHEN);
                    }
                }
            }

            // Namen in der Suite eindeutig machen
            clonedCase.setName(uniqueCaseName(copy, clonedCase.getName()));

            copy.getTestCases().add(clonedCase);
        }

        // In der Root-Liste direkt hinter die Original-Suite hängen
        List<TestSuite> suites = rootModel.getTestSuites();
        int insertIndex = suites.indexOf(src) + 1;
        if (insertIndex <= 0 || insertIndex > suites.size()) {
            insertIndex = suites.size();
        }
        suites.add(insertIndex, copy);

        // UI-Knoten bauen
        TestNode rootUiNode = (TestNode) ((DefaultTreeModel) testTree.getModel()).getRoot();
        // clickedSuite.getParent() ist eigentlich rootUiNode, aber wir gehen defensiv:
        TestNode parentUiNode = (clickedSuite.getParent() instanceof TestNode)
                ? (TestNode) clickedSuite.getParent()
                : rootUiNode;

        TestNode newSuiteNode = new TestNode(copy.getName(), copy);

        // Child-Nodes (Cases + Actions) direkt mit reinbauen, damit UI-Kopie vollständig ist
        for (TestCase tc : copy.getTestCases()) {
            TestNode caseNode = new TestNode(tc.getName(), tc);
            if (tc.getWhen() != null) {
                for (TestAction a : tc.getWhen()) {
                    TestNode stepNode = new TestNode(renderActionLabel(a), a);
                    caseNode.add(stepNode);
                }
            }
            newSuiteNode.add(caseNode);
        }

        // In Swing-Baum einfügen
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        int uiInsertPos = parentUiNode.getIndex(clickedSuite) + 1;
        if (uiInsertPos < 0) {
            uiInsertPos = parentUiNode.getChildCount();
        }
        model.insertNodeInto(newSuiteNode, parentUiNode, uiInsertPos);

        // speichern & neuen Knoten selektieren
        reg.save();
        selectNode(newSuiteNode);
    }

    /** Case duplizieren: Actions tief, Given/Then flach, Name "copy of …" unique in Suite. */
    public void duplicateCaseAfter(TestNode clickedCase) {
        if (clickedCase == null || !(clickedCase.getModelRef() instanceof TestCase)) return;

        // Suite-Knoten im UI besorgen
        DefaultMutableTreeNode suiteNode = (DefaultMutableTreeNode) clickedCase.getParent();
        Object suiteRef = (suiteNode instanceof TestNode) ? ((TestNode) suiteNode).getModelRef() : null;
        if (!(suiteRef instanceof TestSuite)) return;

        TestSuite suite = (TestSuite) suiteRef;
        TestCase src = (TestCase) clickedCase.getModelRef();

        // Deep Clone erstellen
        TestCase copy = cloneCaseDeep(src, true /* prefixedName -> "copy of ..." */);

        // Parent und UUIDs finalisieren
        copy.setParentId(suite.getId());

        // Actions in dem geklonten Case müssen nochmal Parent setzen
        if (copy.getWhen() != null) {
            for (TestAction a : copy.getWhen()) {
                a.setParentId(copy.getId()); // copy.getId() wurde in cloneCaseDeep vergeben
                if (a.getType() == null) {
                    a.setType(TestAction.ActionType.WHEN);
                }
            }
        }

        // Name eindeutig in dieser Suite machen
        copy.setName(uniqueCaseName(suite, copy.getName()));

        // Modell: direkt hinter src einfügen
        List<TestCase> cases = suite.getTestCases();
        int insertIndex = suiteNode.getIndex(clickedCase) + 1;
        if (insertIndex < 0) {
            insertIndex = cases.size();
        }
        cases.add(insertIndex, copy);

        // UI-Knoten bauen
        TestNode newCaseNode = new TestNode(copy.getName(), copy);
        if (copy.getWhen() != null) {
            for (TestAction a : copy.getWhen()) {
                TestNode stepNode = new TestNode(renderActionLabel(a), a);
                newCaseNode.add(stepNode);
            }
        }

        // Im Swing-Baum einfügen
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.insertNodeInto(newCaseNode, suiteNode, insertIndex);
        testTree.expandPath(new TreePath(suiteNode.getPath()));

        // speichern & neuen Knoten selektieren
        TestRegistry.getInstance().save();
        selectNode(newCaseNode);
    }

    /** Step duplizieren: shallow copy (alle bekannten Felder); Insert direkt dahinter. */
    public void duplicateActionAfter(TestNode clickedAction) {
        if (clickedAction == null || !(clickedAction.getModelRef() instanceof TestAction)) return;

        // Case-Knoten holen
        DefaultMutableTreeNode caseNode = (DefaultMutableTreeNode) clickedAction.getParent();
        Object caseRef = (caseNode instanceof TestNode) ? ((TestNode) caseNode).getModelRef() : null;
        if (!(caseRef instanceof TestCase)) return;

        TestCase tc = (TestCase) caseRef;
        TestAction src = (TestAction) clickedAction.getModelRef();

        // Shallow Clone der Action
        TestAction copy = cloneActionShallow(src, true /* tryPrefixName */);
        // Parent setzen
        copy.setParentId(tc.getId());
        if (copy.getType() == null) {
            copy.setType(TestAction.ActionType.WHEN);
        }

        // Modell einfügen direkt hinter src
        List<TestAction> steps = tc.getWhen();
        int insertIndex = caseNode.getIndex(clickedAction) + 1;
        if (insertIndex < 0) {
            insertIndex = steps.size();
        }
        steps.add(insertIndex, copy);

        // neuen UI-Knoten bauen
        TestNode newStepNode = new TestNode(renderActionLabel(copy), copy);

        // Im Swing-Baum einfügen
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.insertNodeInto(newStepNode, caseNode, insertIndex);
        testTree.expandPath(new TreePath(caseNode.getPath()));

        // speichern & neuen Node selektieren
        TestRegistry.getInstance().save();
        selectNode(newStepNode);
    }

    /**
     * Rebuild the entire visible "Tests" tree from the current TestRegistry model.
     *
     * Model-Hierarchie:
     *  RootNode (unsichtbar für den User)
     *    -> TestSuites[]
     *         -> TestCases[]
     *              -> TestActions (WHEN steps)
     *
     * UI-Hierarchie (JTree):
     *   "Testsuites" (reiner UI-Knoten, kein echtes Modelobjekt)
     *      -> suiteNode (modelRef = TestSuite)
     *           -> caseNode (modelRef = TestCase)
     *                -> stepNode (modelRef = TestAction)
     *
     * Dazu setzen wir die neuen/alten Felder (Status, modelRef) wie gehabt.
     */
    public void refreshTestTree() {
        // Hole aktuelle Daten aus Registry
        TestRegistry reg = TestRegistry.getInstance();
        RootNode rootModel = reg.getRoot();

        // UI-Wurzel (sichtbarer Knoten oben im Tree)
        TestNode uiRoot = new TestNode("Testsuites"); // dieser hat KEIN modelRef

        // Für jede Suite im RootModel UI-Knoten bauen
        for (TestSuite suite : rootModel.getTestSuites()) {
            TestNode suiteNode = new TestNode(suite.getName(), suite);

            // Für jeden Case einen Child-Knoten
            for (TestCase testCase : suite.getTestCases()) {
                TestNode caseNode = new TestNode(testCase.getName(), testCase);

                // Für jede Action (WHEN-Step) darunter wieder einen Knoten
                for (TestAction action : testCase.getWhen()) {
                    String label = renderActionLabel(action);
                    TestNode stepNode = new TestNode(label, action);
                    caseNode.add(stepNode);
                }

                suiteNode.add(caseNode);
            }

            uiRoot.add(suiteNode);
        }

        // Das gebaute UI-Root jetzt ins echte Swing-TreeModel hängen
        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.setRoot(uiRoot);
        model.reload();
    }


}
