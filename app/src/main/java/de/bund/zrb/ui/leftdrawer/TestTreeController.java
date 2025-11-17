package de.bund.zrb.ui.leftdrawer;

import de.bund.zrb.event.ApplicationEventBus;
import de.bund.zrb.event.PreconditionSavedEvent;
import de.bund.zrb.event.TestActionUpdatedEvent;
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
 * Controller für den linken Testbaum (LeftDrawer). Verantwortlich für:
 * <ul>
 *   <li>Aufbau und Aktualisierung des Swing-JTree aus dem Test-Datenmodell (TestRegistry)</li>
 *   <li>Selektion, Zustands-Restore (expandierte Knoten / Selektion)</li>
 *   <li>CRUD-Operationen (Erstellen, Duplizieren, Löschen, Verschieben) für Suites, Cases und Actions</li>
 *   <li>In-Place-Umbenennen von TestAction-Knoten (Beschreibung als Label)</li>
 *   <li>Kontextmenü und Hilfsfunktionen zur Modell-Manipulation</li>
 * </ul>
 * Der Controller schreibt Änderungen sofort ins Registry und triggert notwendige UI-Refreshes.
 */
public class TestTreeController {

    private static final String PROP_ACTION_DEFAULT_TIMEOUT = "wd4j.action.defaultTimeoutMillis";
    private static int getActionDefaultTimeoutMillis() {
        String v = System.getProperty(PROP_ACTION_DEFAULT_TIMEOUT);
        if (v == null || v.trim().isEmpty()) return 30000; // Fallback 30s, wie bisher
        try {
            double d = Double.parseDouble(v.trim());
            if (d < 0) return 30000; // keine negativen Werte
            return (int) Math.floor(d);
        } catch (NumberFormatException ex) {
            return 30000;
        }
    }
    private final JTree testTree;

    // Optionaler Handler, um einen Node in einem neuen (persistenten) Tab zu öffnen
    private NodeOpenHandler openHandler;

    public TestTreeController(JTree testTree) {
        this.testTree = testTree;
    }

    public void setOpenHandler(NodeOpenHandler openHandler) {
        this.openHandler = openHandler;
    }

    // ========================= Build & Refresh =========================

    /**
     * Erzeugt einen neuen JTree (nur Struktur, kein Listener) basierend auf dem aktuellen RootNode aus der TestRegistry.
     * Wird typischerweise beim Initialisieren des LeftDrawer verwendet.
     * @return neuer JTree mit gesetztem CellRenderer.
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
     * Aktualisiert den existierenden {@link JTree} dieses Controllers vollständig aus dem aktuellen Modell.
     * <p>Vor dem Austausch der Baumwurzel werden expandierte Knoten und die aktuelle Selektion gesichert
     * und nach dem Reload wiederhergestellt.</p>
     */
    public void refreshTreeFromRegistry() {
        // Zustand sichern
        Set<String> expanded = captureExpandedKeys();
        String selectedKey = captureSelectedKey();

        TestRegistry reg = TestRegistry.getInstance();
        RootNode rootModel = reg.getRoot();

        TestNode newRootUi = buildUiTreeFromModel(rootModel);

        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.setRoot(newRootUi);
        model.reload();

        // Zustand wiederherstellen
        restoreExpanded(expanded);
        restoreSelection(selectedKey);
    }

    /**
     * Hilfsfunktion: nimmt RootNode/TestSuite/TestCase/TestAction
     * und erzeugt die Swing-Nodes (TestNode) mit korrekten Labels.
     * Diese Methode erzeugt KEINE Side-Effects am echten JTree sondern liefert nur die neue Baumwurzel.
     * @param rootModel RootNode aus der Registry (kann null sein)
     * @return UI-TestNode als Wurzel des Baumes
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

    /**
     * Liefert den sichtbaren Label-Text für eine Action. Falls eine Beschreibung gesetzt ist, wird diese
     * bevorzugt als alleiniger Text gezeigt. Andernfalls wird der Action-Name ergänzt um Value oder SelectedSelector.
     * @param action TestAction Instanz
     * @return aufbereiteter Label-Text
     */
    public static String renderActionLabel(TestAction action) {
        // NEU: Wenn description gesetzt, verwende sie als alleinigen Labeltext
        if (action.getDescription() != null && action.getDescription().trim().length() > 0) {
            return action.getDescription().trim();
        }
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
     * Initialisiert das Kontextmenü und registriert Maus- und Tastatur-Handler.
     * Aktiviert In-Place-Editing per Doppelklick oder F2 für TestAction-Knoten.
     */
    public void setupContextMenu() {
        // kein statisches PopupMenu direkt am JTree setzen (wir bauen dynamisch)
        testTree.setComponentPopupMenu(null);
        // In-Place Editing nur für TestAction aktivieren
        testTree.setEditable(true);
        testTree.setCellEditor(new ActionNodeCellEditor(testTree));
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
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    TreePath path = testTree.getPathForLocation(e.getX(), e.getY());
                    if (path == null) return;
                    Object comp = path.getLastPathComponent();
                    if (comp instanceof TestNode && ((TestNode) comp).getModelRef() instanceof TestAction) {
                        testTree.startEditingAtPath(path);
                    }
                }
            }
        });
        // F2: In-Place-Umbenennen via KeyBinding (robuster als KeyListener)
        javax.swing.InputMap imF = testTree.getInputMap(JComponent.WHEN_FOCUSED);
        javax.swing.InputMap imA = testTree.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        javax.swing.KeyStroke ksF2 = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F2, 0);
        String actionKey = "wd4j.renameInPlace";
        if (imF != null) imF.put(ksF2, actionKey);
        if (imA != null) imA.put(ksF2, actionKey);
        javax.swing.ActionMap am = testTree.getActionMap();
        am.put(actionKey, new javax.swing.AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) {
                TreePath sel = testTree.getSelectionPath();
                if (sel == null) return;
                Object comp = sel.getLastPathComponent();
                if (comp instanceof TestNode && ((TestNode) comp).getModelRef() instanceof TestAction) {
                    testTree.startEditingAtPath(sel);
                }
            }
        });
    }

    /**
     * Baut das dynamische Kontextmenü für den übergebenen Knoten.
     * Menüpunkte hängen vom konkreten Modelltyp (Root, Suite, Case, Action) ab.
     * @param clicked aktuell angeklickter UI-Knoten (kann null sein)
     * @return Popup-Menü zur Anzeige
     */
    public JPopupMenu buildContextMenu(TestNode clicked) {
        JPopupMenu menu = new JPopupMenu();

        if (clicked == null) {
            JMenuItem newSuite = new JMenuItem("Neue Testsuite");
            newSuite.addActionListener(evt -> createNewSuiteAtFront());
            menu.add(newSuite);
            return menu;
        }

        Object ref = clicked.getModelRef();

        // RootNode: Neue Suite direkt vorne
        if (ref instanceof RootNode) {
            JMenuItem newSuiteFront = new JMenuItem("Neue Testsuite");
            newSuiteFront.addActionListener(evt -> createNewSuiteAtFront());
            menu.add(newSuiteFront);
            menu.addSeparator();
        }

        if (openHandler != null && (ref instanceof RootNode || ref instanceof TestSuite || ref instanceof TestCase || ref instanceof TestAction)) {
            JMenuItem openPersistent = new JMenuItem("In neuem Tab öffnen");
            openPersistent.addActionListener(evt -> openHandler.openInNewTab(clicked));
            menu.add(openPersistent);
            menu.addSeparator();
        }

        if (ref instanceof TestSuite) {
            JMenuItem newCaseFront = new JMenuItem("Neuer TestCase");
            newCaseFront.addActionListener(evt -> createNewCaseUnderSuite(clicked));
            menu.add(newCaseFront);

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
            JMenuItem newStepFront = new JMenuItem("Neuer Schritt");
            newStepFront.addActionListener(evt -> createNewStepUnderCase(clicked));
            menu.add(newStepFront);

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
            JMenuItem newStep = new JMenuItem("Neuer Schritt nach diesem");
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

    /**
     * Fügt allgemeine Aktionen (Umbenennen, Löschen, Eigenschaften) zum Kontextmenü hinzu.
     * @param menu Popup-Menü Instanz
     * @param onNode betroffener Knoten (optional, derzeit nicht genutzt)
     */
    public void addCommonMenuItems(JPopupMenu menu, TestNode onNode) {
        Object modelRef = onNode != null ? onNode.getModelRef() : null;

        if (modelRef instanceof TestAction) {
            // In-Place-Umbenennen (Description bearbeiten) als "Umbenennen"
            JMenuItem inplaceRename = new JMenuItem("Umbenennen");
            inplaceRename.addActionListener(evt -> {
                if (onNode == null) return;
                TreePath path = new TreePath(onNode.getPath());
                testTree.setSelectionPath(path);
                testTree.startEditingAtPath(path);
            });
            menu.add(inplaceRename);

            // Alte Aktion-Ändern-Logik (ActionPicker) als "Aktion ändern"
            JMenuItem changeAction = new JMenuItem("Aktion ändern");
            changeAction.addActionListener(evt -> renameNode());
            menu.add(changeAction);
        } else {
            // Standard-Umbenennen für Suite/Case (Dialog)
            JMenuItem renameItem = new JMenuItem("Umbenennen");
            renameItem.addActionListener(evt -> renameNode());
            menu.add(renameItem);
        }

        JMenuItem deleteItem = new JMenuItem("Löschen");
        deleteItem.addActionListener(evt -> deleteNode());
        menu.add(deleteItem);

        menu.addSeparator();
        JMenuItem propertiesItem = new JMenuItem("Eigenschaften");
        propertiesItem.addActionListener(evt -> openPropertiesDialog());
        menu.add(propertiesItem);
    }

    // ========================= Aktionen: Suite/Case/Step verschieben als Precondition =========================

    /**
     * Verschiebt einen TestCase in eine neue Precondition. Der Case wird aus seiner Suite entfernt,
     * alle When-Schritte werden shallow kopiert und als Actions der Precondition übernommen.
     * Persistiert Änderungen und aktualisiert den Baum.
     * @param clickedCase UI-Knoten eines TestCase
     */
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

    /**
     * Verschiebt eine gesamte Testsuite in eine neue Precondition. Alle Actions aller Cases werden zusammengeführt.
     * Danach wird die Suite aus dem Root entfernt und gespeichert.
     * @param clickedSuite UI-Knoten einer Testsuite
     */
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

    /**
     * Erzeugt aus einem TestCase eine neue Precondition (nur WHEN-Schritte). Andere Scopes / Templates werden ignoriert.
     * @param src Ursprünglicher TestCase
     * @param name Name der neuen Precondition
     * @return erzeugte Precondition
     */
    private de.bund.zrb.model.Precondition buildPreconditionFromCase(TestCase src, String name) {
        de.bund.zrb.model.Precondition p = PreconditionFactory.newPrecondition(name);

        // KEINE Scopes kopieren (src.getBefore(), src.getTemplates(), etc.)
        // Nur ausführbare Schritte übernehmen.

        if (src.getWhen() != null) {
            for (TestAction a : src.getWhen()) {
                p.getActions().add(cloneActionShallow(a, false));
            }
        }
        return p;
    }

    /**
     * Erzeugt aus einer Testsuite eine neue Precondition (aggregiert alle Actions aller Cases).
     * @param src Quell-Suite
     * @param name Zielname der Precondition
     * @return neue Precondition
     */
    private de.bund.zrb.model.Precondition buildPreconditionFromSuite(TestSuite src, String name) {
        de.bund.zrb.model.Precondition p = PreconditionFactory.newPrecondition(name);

        // KEINE suite.getGiven() mehr kopieren.
        // Wir sammeln nur die Actions aller Cases, in Reihenfolge.

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
     * Legt einen neuen TestCase hinter dem ausgewählten Case an.
     * Fragt den Namen per Dialog ab, erzeugt das Modellobjekt, speichert und refresht den Baum.
     * @param caseNode aktuell selektierter Case-Knoten
     */
    public void createNewCase(TestNode caseNode) {
        // Sicherheitscheck: wir erlauben "neuen Case" nur, wenn ein Case angeklickt wurde
        if (caseNode == null || !(caseNode.getModelRef() instanceof TestCase)) {
            return;
        }

        // Name abfragen
        String name = JOptionPane.showInputDialog(
                testTree,
                "Name des neuen TestCase:",
                "Neuer TestCase",
                JOptionPane.PLAIN_MESSAGE
        );
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        String trimmedName = name.trim();

        // Modell holen
        TestRegistry reg = TestRegistry.getInstance();
        RootNode rootModel = reg.getRoot();

        // Die Suite finden, in der der angeklickte Case liegt
        TestNode suiteNode = (TestNode) caseNode.getParent();
        if (suiteNode == null || !(suiteNode.getModelRef() instanceof TestSuite)) {
            return;
        }
        TestSuite suiteModel = (TestSuite) suiteNode.getModelRef();

        List<TestCase> cases = suiteModel.getTestCases();
        if (cases == null) {
            // sollte eigentlich nie null sein, aber wir sind defensiv
            // falls du KEIN setTestCases() hast, dann hast du sie hoffentlich final initialisiert im POJO :)
            // Wenn du DOCH ein setTestCases(...) hast, kannst du's hier auf new ArrayList<>() setzen.
        }

        // Neuen Case bauen
        TestCase newCase = new TestCase(trimmedName, new ArrayList<TestAction>());
        newCase.setId(newUuid());
        newCase.setParentId(suiteModel.getId());

        // Einfüge-Index bestimmen: hinter dem aktuell angeklickten Case
        TestCase clickedCaseModel = (TestCase) caseNode.getModelRef();
        int insertIdx = cases.indexOf(clickedCaseModel);
        if (insertIdx < 0) {
            insertIdx = cases.size() - 1;
        }
        insertIdx = insertIdx + 1;
        if (insertIdx < 0 || insertIdx > cases.size()) {
            insertIdx = cases.size();
        }

        cases.add(insertIdx, newCase);

        // Speichern
        reg.save();

        // UI neu aufbauen
        refreshTestTree();

        // neuen Case selektieren
        selectNodeByModelRef(newCase);
    }

    /**
     * Legt eine neue TestAction hinter der ausgewählten Action an (selber Case).
     * Fragt den Action-Typ über den ActionPickerDialog ab.
     * @param stepNode UI-Knoten einer bestehenden TestAction
     */
    public void createNewStep(TestNode stepNode) {
        if (stepNode == null || !(stepNode.getModelRef() instanceof TestAction)) {
            return;
        }

        // Actiontyp über Dialog erfragen (gleich wie vorher)
        Window owner = SwingUtilities.getWindowAncestor(testTree);
        ActionPickerDialog dlg = new ActionPickerDialog(owner, "Neuer Schritt", "click");
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) {
            return;
        }

        String actionName = dlg.getChosenAction();
        if (actionName == null || actionName.length() == 0) {
            return;
        }

        // Modell holen
        TestRegistry reg = TestRegistry.getInstance();

        // Den Case finden, unter dem der Step hängt
        TestNode caseNode = (TestNode) stepNode.getParent();
        if (caseNode == null || !(caseNode.getModelRef() instanceof TestCase)) {
            return;
        }
        TestCase caseModel = (TestCase) caseNode.getModelRef();

        List<TestAction> steps = caseModel.getWhen();
        if (steps == null) {
            steps = new ArrayList<>();
            // wenn du einen Setter hast: caseModel.setWhen(steps);
        }

        TestAction clickedAction = (TestAction) stepNode.getModelRef();

        // neue Action bauen
        TestAction newAction = new TestAction();
        newAction.setId(newUuid());
        newAction.setParentId(caseModel.getId());
        newAction.setAction(actionName);
        newAction.setType(TestAction.ActionType.WHEN);
        newAction.setTimeout(getActionDefaultTimeoutMillis()); // live lesen
        // rest (user, locator, etc.) lassen wir leer/0 erst mal

        // Einfügeposition bestimmen (hinter geklicktem Step)
        int insertIdx = steps.indexOf(clickedAction);
        if (insertIdx < 0) {
            insertIdx = steps.size() - 1;
        }
        insertIdx = insertIdx + 1;
        if (insertIdx < 0 || insertIdx > steps.size()) {
            insertIdx = steps.size();
        }

        steps.add(insertIdx, newAction);

        // Speichern
        reg.save();

        // UI refreshen
        refreshTestTree();

        // neu eingefügten Step selektieren
        selectNodeByModelRef(newAction);
    }

    /** Hilfsfunktion für UUIDs, damit's lesbarer ist. */
    private String newUuid() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * Legt eine neue Testsuite hinter der ausgewählten Suite an.
     * @param clickedSuiteNode UI-Knoten der Referenz-Suite; kann null sein (dann ans Ende)
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
     * Legt eine neue Testsuite am Anfang der Suite-Liste an.
     */
    private void createNewSuiteAtFront() {
        String name = JOptionPane.showInputDialog(
                testTree,
                "Name der neuen Testsuite:",
                "Neue Testsuite",
                JOptionPane.PLAIN_MESSAGE
        );
        if (name == null || name.trim().isEmpty()) return;
        String trimmed = name.trim();
        TestRegistry reg = TestRegistry.getInstance();
        RootNode rootModel = reg.getRoot();
        List<TestSuite> suites = rootModel.getTestSuites();
        if (suites == null) {
            suites = new ArrayList<>();
            rootModel.setTestSuites(suites);
        }
        TestSuite newSuite = new TestSuite(trimmed, new ArrayList<>());
        newSuite.setId(newUuid());
        newSuite.setParentId(rootModel.getId());
        newSuite.setDescription("");
        suites.add(0, newSuite);
        reg.save();
        refreshTestTree();
        selectNodeByModelRef(newSuite);
    }

    /**
     * Legt einen neuen TestCase am Anfang einer Suite an.
     * @param suiteNode Suite-Knoten, unter dem der Case eingefügt wird
     */
    private void createNewCaseUnderSuite(TestNode suiteNode) {
        if (suiteNode == null || !(suiteNode.getModelRef() instanceof TestSuite)) return;
        String name = JOptionPane.showInputDialog(
                testTree,
                "Name des neuen TestCase:",
                "Neuer TestCase",
                JOptionPane.PLAIN_MESSAGE
        );
        if (name == null || name.trim().isEmpty()) return;
        String trimmed = name.trim();
        TestSuite suiteModel = (TestSuite) suiteNode.getModelRef();
        List<TestCase> cases = suiteModel.getTestCases();
        if (cases == null) {
            cases = new ArrayList<>();
            // falls Setter vorhanden: suiteModel.setTestCases(cases);
        }
        TestCase newCase = new TestCase(trimmed, new ArrayList<>());
        newCase.setId(newUuid());
        newCase.setParentId(suiteModel.getId());
        cases.add(0, newCase);
        TestRegistry.getInstance().save();
        refreshTestTree();
        selectNodeByModelRef(newCase);
    }

    /**
     * Legt eine neue TestAction am Anfang eines TestCase an.
     * @param caseNode Case-Knoten
     */
    private void createNewStepUnderCase(TestNode caseNode) {
        if (caseNode == null || !(caseNode.getModelRef() instanceof TestCase)) return;
        Window owner = SwingUtilities.getWindowAncestor(testTree);
        ActionPickerDialog dlg = new ActionPickerDialog(owner, "Neuer Schritt", "click");
        dlg.setVisible(true);
        if (!dlg.isConfirmed()) return;
        String actionName = dlg.getChosenAction();
        if (actionName == null || actionName.isEmpty()) return;
        TestCase caseModel = (TestCase) caseNode.getModelRef();
        List<TestAction> steps = caseModel.getWhen();
        if (steps == null) {
            steps = new ArrayList<>();
            // falls Setter vorhanden: caseModel.setWhen(steps);
        }
        TestAction newAction = new TestAction();
        newAction.setId(newUuid());
        newAction.setParentId(caseModel.getId());
        newAction.setAction(actionName);
        newAction.setType(TestAction.ActionType.WHEN);
        newAction.setTimeout(getActionDefaultTimeoutMillis());
        steps.add(0, newAction);
        TestRegistry.getInstance().save();
        refreshTestTree();
        selectNodeByModelRef(newAction);
    }

    /**
     * Wählt einen UI-Knoten basierend auf der Modell-Referenz wieder aus (z.B. nach Refresh).
     * @param targetRef Modellobjekt, das ausgewählt werden soll
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

    /**
     * Rekursive Suche nach dem ersten Knoten, dessen modelRef identisch mit target ist.
     * @param current Startknoten
     * @param target Modellreferenz (Identität, kein equals Vergleich)
     * @return gefundener Knoten oder null
     */
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

    /**
     * Öffnet den passenden Umbenennen-Dialog je nach Modelltyp (Suite / Case / Action).
     * Für Actions wird der ActionPickerDialog verwendet. Speichert und refresht bei Erfolg.
     */
    public void renameNode() {
        TestNode selected = getSelectedNode();
        if (selected == null || selected.getParent() == null) {
            // Root nicht umbenennen
            return;
        }

        Object ref = selected.getModelRef();
        TestRegistry reg = TestRegistry.getInstance();

        if (ref instanceof TestSuite) {
            TestSuite suite = (TestSuite) ref;
            String current = safeName(suite.getName());
            String name = JOptionPane.showInputDialog(
                    testTree,
                    "Neuer Name:",
                    current
            );
            if (name == null || name.trim().isEmpty()) {
                return;
            }
            suite.setName(name.trim());

            reg.save();
            refreshTestTree();
            selectNodeByModelRef(suite);
            return;
        }

        if (ref instanceof TestCase) {
            TestCase tc = (TestCase) ref;
            String current = safeName(tc.getName());
            String name = JOptionPane.showInputDialog(
                    testTree,
                    "Neuer Name:",
                    current
            );
            if (name == null || name.trim().isEmpty()) {
                return;
            }
            tc.setName(name.trim());

            reg.save();
            refreshTestTree();
            selectNodeByModelRef(tc);
            return;
        }

        if (ref instanceof TestAction) {
            TestAction a = (TestAction) ref;

            // alter Dialog wie gehabt
            Window owner = SwingUtilities.getWindowAncestor(testTree);
            ActionPickerDialog dlg = new ActionPickerDialog(owner,
                    "Step umbenennen (Action)",
                    a.getAction());
            dlg.setVisible(true);
            if (!dlg.isConfirmed()) {
                return;
            }

            String newAction = dlg.getChosenAction();
            if (newAction == null || newAction.length() == 0) {
                return;
            }

            a.setAction(newAction);

            reg.save();
            refreshTestTree();
            selectNodeByModelRef(a);
            return;
        }

        // Fallback: nichts tun
    }

    /**
     * Löscht den aktuell selektierten Knoten (Suite, Case oder Action) nach Sicherheitsabfrage.
     * Passt das Modell an, persistiert und aktualisiert den Baum. Root kann nicht gelöscht werden.
     */
    public void deleteNode() {
        TestNode selectedNode = getSelectedNode();
        if (selectedNode == null) {
            return;
        }
        // Root darf nicht gelöscht werden
        if (selectedNode.getParent() == null) {
            return;
        }

        Object ref = selectedNode.getModelRef();
        TestNode parentNode = (TestNode) selectedNode.getParent();
        Object parentRef = (parentNode != null ? parentNode.getModelRef() : null);

        TestRegistry reg = TestRegistry.getInstance();
        RootNode rootModel = reg.getRoot();

        // Sicherheitsabfrage wie bisher (optional, aber nett)
        if (!confirmDelete(ref)) {
            return;
        }

        boolean changed = false;

        // ================= Suite löschen =================
        if (ref instanceof TestSuite) {
            TestSuite suiteModel = (TestSuite) ref;
            // aus root entfernen
            List<TestSuite> suites = rootModel.getTestSuites();
            if (suites != null) {
                changed = suites.remove(suiteModel) || changed;
            }
        }

        // ================= Case löschen =================
        else if (ref instanceof TestCase && parentRef instanceof TestSuite) {
            TestCase tcModel = (TestCase) ref;
            TestSuite suiteModel = (TestSuite) parentRef;

            List<TestCase> cases = suiteModel.getTestCases();
            if (cases != null) {
                changed = cases.remove(tcModel) || changed;
            }
        }

        // ================= Action löschen =================
        else if (ref instanceof TestAction && parentRef instanceof TestCase) {
            TestAction actionModel = (TestAction) ref;
            TestCase tcModel = (TestCase) parentRef;

            List<TestAction> steps = tcModel.getWhen();
            if (steps != null) {
                changed = steps.remove(actionModel) || changed;
            }
        }

        // (GivenCondition / ThenExpectation würdest du später analog behandeln.
        //  Aktuell sind die nicht mehr einzeln als Tree-knoten sichtbar,
        //  also lassen wir sie raus.)

        if (changed) {
            // persistieren
            reg.save();
        }

        // UI neu aufbauen
        refreshTestTree();

        // Fallback-Selektionslogik:
        // versuch den Parent im neu aufgebauten Tree zu selektieren
        if (parentRef != null) {
            selectNodeByModelRef(parentRef);
        }
    }

    /**
     * Kleine Confirm-Box. Du kannst sie weglassen, wenn es dich nervt.
     * Wenn du später "Silent Delete" willst: gib einfach immer true zurück.
     * @param ref Modellreferenz des zu löschenden Objekts
     * @return true wenn bestätigt, sonst false
     */
    private boolean confirmDelete(Object ref) {
        String what;
        if (ref instanceof TestSuite) {
            what = "diese Testsuite";
        } else if (ref instanceof TestCase) {
            what = "diesen TestCase";
        } else if (ref instanceof TestAction) {
            what = "diesen Schritt";
        } else {
            what = "dieses Element";
        }

        int opt = JOptionPane.showConfirmDialog(
                testTree,
                "Wirklich " + what + " löschen?",
                "Löschen bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return opt == JOptionPane.YES_OPTION;
    }

    // ========================= Misc / UI Helpers =========================

    /**
     * Öffnet den PropertiesDialog für den aktuell ausgewählten Knoten (sofern kein Root).
     */
    public void openPropertiesDialog() {
        DefaultMutableTreeNode selected = getSelectedNode();
        if (selected != null && selected.getParent() != null) {
            PropertiesDialog dialog = new PropertiesDialog(selected.toString());
            dialog.setVisible(true);
        }
    }

    /**
     * @return Den zuletzt selektierten TestNode oder null.
     */
    public TestNode getSelectedNode() {
        return (TestNode) testTree.getLastSelectedPathComponent();
    }

    /**
     * Setzt den Status eines Knoten (PASSED/FAILED) und propagiert die Aggregation auf seine Suite-Eltern.
     * @param node betroffener Knoten
     * @param passed true wenn bestanden
     */
    public void updateNodeStatus(TestNode node, boolean passed) {
        node.setStatus(passed ? TestNode.Status.PASSED : TestNode.Status.FAILED);

        ((DefaultTreeModel) testTree.getModel()).nodeChanged(node);

        DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
        if (parent instanceof TestNode) {
            updateSuiteStatus((TestNode) parent);
        }
    }

    /**
     * Aggregiert den Status einer Suite über ihre direkten Kinder (FAIL gewinnt).
     * @param suite TestNode einer testsuite
     */
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

    /**
     * @return Root-Knoten des aktuellen JTree
     */
    public TestNode getRootNode() {
        return (TestNode) testTree.getModel().getRoot();
    }

    /**
     * Liefert selektierten Knoten oder Root, falls keine Selektion vorhanden ist.
     * @return ausgewählter oder Root-Knoten
     */
    public DefaultMutableTreeNode getSelectedNodeOrRoot() {
        DefaultMutableTreeNode selected = getSelectedNode();
        return selected != null ? selected : (DefaultMutableTreeNode) testTree.getModel().getRoot();
    }

    /**
     * Liefert alle selektierten Suites; falls keine Selektion, alle Suites im Root.
     * @return Liste von TestSuite
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

    /**
     * Shallow-Klon einer TestAction (IDs neu, lokales Mapping kopiert). Optional kann ein Name-Präfix gesetzt werden.
     * @param src Quell-Action
     * @param tryPrefixName falls true wird ein evtl. vorhandener Name mit Präfix versehen
     * @return neue TestAction
     */
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

    /**
     * Wandelt einen potenziell leeren Namen in einen lesbaren Ersatz ("unnamed").
     * @param s Original-String
     * @return gereinigter Name oder Ersatz
     */
    public String safeName(String s) {
        return (s == null || s.trim().isEmpty()) ? "unnamed" : s.trim();
    }

    /**
     * Erzeugt einen eindeutigen Suite-Namen basierend auf einem Basiswert.
     * @param base Basisname
     * @return eindeutiger Name
     */
    public String uniqueSuiteName(String base) {
        List<TestSuite> suites = TestRegistry.getInstance().getRoot().getTestSuites();
        Set<String> used = new HashSet<String>();
        for (TestSuite s : suites) used.add(safeName(s.getName()));
        return makeUnique(base, used);
    }

    /**
     * Erzeugt einen eindeutigen Case-Namen innerhalb einer Suite.
     * @param suite Suite-Kontext
     * @param base Basisname
     * @return eindeutiger Name
     */
    public String uniqueCaseName(TestSuite suite, String base) {
        Set<String> used = new HashSet<String>();
        for (TestCase c : suite.getTestCases()) used.add(safeName(c.getName()));
        return makeUnique(base, used);
    }

    /**
     * Fügt Zähler-Suffixe ("(2)", "(3)", ...) hinzu bis der Name nicht verwendet ist.
     * @param base Basisname
     * @param used Menge bereits verwendeter Namen
     * @return eindeutiger Name
     */
    public String makeUnique(String base, Set<String> used) {
        String b = safeName(base);
        if (!used.contains(b)) return b;
        for (int i = 2; i < 10000; i++) {
            String cand = b + " (" + i + ")";
            if (!used.contains(cand)) return cand;
        }
        return b + " (copy)";
    }

    /**
     * Versucht per Reflection einen getName()-Aufruf auf dem Objekt.
     * @param bean beliebiges Objekt
     * @return Name oder null
     */
    public String getNameIfExists(Object bean) {
        try {
            java.lang.reflect.Method m = bean.getClass().getMethod("getName");
            Object v = m.invoke(bean);
            return (v != null) ? String.valueOf(v) : null;
        } catch (Exception ignore) { return null; }
    }
    /**
     * Setzt per Reflection einen Namen über setName(String) falls vorhanden.
     * @param bean Zielobjekt
     * @param name neuer Name
     */
    public void setNameIfExists(Object bean, String name) {
        try {
            java.lang.reflect.Method m = bean.getClass().getMethod("setName", String.class);
            m.invoke(bean, name);
        } catch (Exception ignore) { /* kein Name vorhanden */ }
    }

    /**
     * Selektiert einen TestNode im JTree, expandiert ggf. dessen Parent und scrollt ihn sichtbar.
     * @param node Zielknoten
     */
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

    /**
     * Dupliziert eine Suite (tief), fügt sie direkt hinter das Original ein und selektiert sie.
     * @param clickedSuiteNode UI-Knoten der zu duplizierenden Suite
     */
    public void duplicateSuiteAfter(TestNode clickedSuiteNode) {
        if (clickedSuiteNode == null) return;
        Object r = clickedSuiteNode.getModelRef();
        if (!(r instanceof TestSuite)) return;

        TestSuite originalSuite = (TestSuite) r;
        TestRegistry reg = TestRegistry.getInstance();
        RootNode rootModel = reg.getRoot();

        // 1. Deep Clone
        TestSuite clone = cloneSuiteDeepForDuplicate(originalSuite, rootModel);

        // 2. Direkt nach original einfügen
        insertSuiteCopyAfter(originalSuite, clone, rootModel);

        // 3. persist + UI refresh
        reg.save();
        refreshTestTree();

        // 4. neu selektieren
        selectNodeByModelRef(clone);
    }

    /**
     * Dupliziert einen TestCase innerhalb seiner Suite (tief) und selektiert ihn.
     * @param clickedCaseNode Case-Knoten
     */
    public void duplicateCaseAfter(TestNode clickedCaseNode) {
        if (clickedCaseNode == null) return;

        Object r = clickedCaseNode.getModelRef();
        if (!(r instanceof TestCase)) return;
        TestCase originalCase = (TestCase) r;

        // parent Suite ermitteln (aus Tree & Model)
        TestNode parentNode = (TestNode) clickedCaseNode.getParent();
        if (parentNode == null) return;
        Object parentRef = parentNode.getModelRef();
        if (!(parentRef instanceof TestSuite)) return;
        TestSuite parentSuite = (TestSuite) parentRef;

        // 1. Deep Clone
        TestCase clone = cloneCaseDeepForDuplicate(originalCase, parentSuite.getId());

        // Name eindeutig innerhalb dieser Suite machen
        clone.setName(uniqueCaseName(parentSuite, clone.getName()));

        // 2. Direkt nach Original einfügen
        insertCaseCopyAfter(originalCase, clone, parentSuite);

        // 3. persist + UI refresh
        TestRegistry reg = TestRegistry.getInstance();
        reg.save();
        refreshTestTree();

        // 4. neu selektieren
        selectNodeByModelRef(clone);
    }

    /**
     * Dupliziert eine TestAction shallow und fügt sie direkt dahinter ein.
     * @param clickedActionNode Action-Knoten
     */
    public void duplicateActionAfter(TestNode clickedActionNode) {
        if (clickedActionNode == null) return;

        Object r = clickedActionNode.getModelRef();
        if (!(r instanceof TestAction)) return;
        TestAction originalAction = (TestAction) r;

        // parent Case ermitteln
        TestNode parentNode = (TestNode) clickedActionNode.getParent();
        if (parentNode == null) return;
        Object parentRef = parentNode.getModelRef();
        if (!(parentRef instanceof TestCase)) return;
        TestCase parentCase = (TestCase) parentRef;

        // 1. Clone shallow mit neuer ID
        TestAction clone = cloneActionShallowForDuplicate(originalAction, parentCase.getId());

        // 2. Direkt nach Original einfügen
        insertActionCopyAfter(originalAction, clone, parentCase);

        // 3. persist + UI refresh
        TestRegistry reg = TestRegistry.getInstance();
        reg.save();
        refreshTestTree();

        // 4. neu selektieren
        selectNodeByModelRef(clone);
    }

    /**
     * Baut den sichtbaren Testbaum komplett neu aus der Registry (ähnlich refreshTreeFromRegistry, aber lokal)
     * und versucht Expansions / Selektion wiederherzustellen.
     */
    public void refreshTestTree() {
        // Zustand sichern
        Set<String> expanded = captureExpandedKeys();
        String selectedKey = captureSelectedKey();

        // Hole das echte Modell
        RootNode rootModel = TestRegistry.getInstance().getRoot();

        // Baue einen UI-Node für Root und hänge das RootModel dran
        TestNode uiRoot = new TestNode("Testsuites", rootModel);

        // Darunter: alle Suites als Kinder
        List<TestSuite> suites = rootModel.getTestSuites();
        for (TestSuite suite : suites) {
            TestNode suiteNode = new TestNode(suite.getName(), suite);

            // Cases drunter
            for (TestCase testCase : suite.getTestCases()) {
                TestNode caseNode = new TestNode(testCase.getName(), testCase);

                // Actions drunter
                for (TestAction action : testCase.getWhen()) {
                    String label = renderActionLabel(action);
                    TestNode stepNode = new TestNode(label, action);
                    caseNode.add(stepNode);
                }

                suiteNode.add(caseNode);
            }

            uiRoot.add(suiteNode);
        }

        DefaultTreeModel model = (DefaultTreeModel) testTree.getModel();
        model.setRoot(uiRoot);
        model.reload();

        // Zustand wiederherstellen (inkl. Root)
        restoreExpanded(expanded);
        if (expanded.isEmpty()) {
            testTree.expandPath(new TreePath(uiRoot.getPath()));
        }
        restoreSelection(selectedKey);
    }

    // ===== Expand/Selection State Helpers =====
    private Set<String> captureExpandedKeys() {
        Set<String> out = new java.util.HashSet<>();
        int rows = testTree.getRowCount();
        for (int i = 0; i < rows; i++) {
            TreePath p = testTree.getPathForRow(i);
            if (p == null) continue;
            Object last = p.getLastPathComponent();
            if (!(last instanceof TestNode)) continue;
            String key = keyForModel(((TestNode) last).getModelRef());
            if (key != null && testTree.isExpanded(p)) out.add(key);
        }
        return out;
    }

    private String captureSelectedKey() {
        TestNode sel = getSelectedNode();
        if (sel == null) return null;
        return keyForModel(sel.getModelRef());
    }

    private void restoreExpanded(Set<String> keys) {
        if (keys == null || keys.isEmpty()) return;
        Object rootObj = ((DefaultTreeModel) testTree.getModel()).getRoot();
        if (!(rootObj instanceof TestNode)) return;
        TestNode root = (TestNode) rootObj;
        expandMatchingRecursive(root, keys);
    }

    private void expandMatchingRecursive(TestNode node, Set<String> keys) {
        String key = keyForModel(node.getModelRef());
        if (key != null && keys.contains(key)) {
            testTree.expandPath(new TreePath(node.getPath()));
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            Object ch = node.getChildAt(i);
            if (ch instanceof TestNode) expandMatchingRecursive((TestNode) ch, keys);
        }
    }

    private void restoreSelection(String key) {
        if (key == null) return;
        Object rootObj = ((DefaultTreeModel) testTree.getModel()).getRoot();
        if (!(rootObj instanceof TestNode)) return;
        TestNode root = (TestNode) rootObj;
        TestNode match = findNodeByKey(root, key);
        if (match != null) selectNode(match);
    }

    private TestNode findNodeByKey(TestNode node, String key) {
        String k = keyForModel(node.getModelRef());
        if (key.equals(k)) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            Object ch = node.getChildAt(i);
            if (ch instanceof TestNode) {
                TestNode found = findNodeByKey((TestNode) ch, key);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Erzeugt einen Schlüssel (Typ:ID) für ein Modellobjekt, um Expansions/Selektion stabil zu halten.
     * @param ref Modellreferenz
     * @return Schlüssel oder null
     */
    private String keyForModel(Object ref) {
        if (ref instanceof RootNode) {
            String id = ((RootNode) ref).getId();
            return id != null ? ("root:" + id) : "root";
        }
        if (ref instanceof TestSuite) {
            String id = ((TestSuite) ref).getId();
            return id != null ? ("suite:" + id) : null;
        }
        if (ref instanceof TestCase) {
            String id = ((TestCase) ref).getId();
            return id != null ? ("case:" + id) : null;
        }
        if (ref instanceof TestAction) {
            String id = ((TestAction) ref).getId();
            return id != null ? ("act:" + id) : null;
        }
        return null;
    }

    // ==== Missing helpers restored (clone/insert for duplicate operations) ====

    private TestAction cloneActionShallowForDuplicate(TestAction src, String newParentCaseId) {
        if (src == null) return null;
        TestAction copy = new TestAction();
        copy.setId(UUID.randomUUID().toString());
        copy.setParentId(newParentCaseId);

        // Metadaten
        copy.setUser(src.getUser());
        copy.setType(src.getType() != null ? src.getType() : TestAction.ActionType.WHEN);
        copy.setSelected(false);

        // Playback-Infos
        copy.setAction(src.getAction());
        copy.setSelectedSelector(src.getSelectedSelector());
        copy.setLocatorType(src.getLocatorType());
        copy.setTimeout(src.getTimeout());
        copy.setValue(src.getValue());

        // Locator-Hints und Extraktionen
        copy.setLocators(new LinkedHashMap<>(src.getLocators()));
        copy.setExtractedValues(new LinkedHashMap<>(src.getExtractedValues()));
        copy.setExtractedAttributes(new LinkedHashMap<>(src.getExtractedAttributes()));
        copy.setExtractedTestIds(new LinkedHashMap<>(src.getExtractedTestIds()));
        copy.setExtractedAriaRoles(new LinkedHashMap<>(src.getExtractedAriaRoles()));
        copy.setRaw(src.getRaw());
        copy.setText(src.getText());
        copy.setRole(src.getRole());
        copy.setLabel(src.getLabel());
        return copy;
    }

    private TestCase cloneCaseDeepForDuplicate(TestCase original, String newParentSuiteId) {
        if (original == null) return null;
        TestCase copy = new TestCase();
        copy.setId(UUID.randomUUID().toString());
        copy.setParentId(newParentSuiteId);

        // Name (vorläufig, wird ggf. später unique gemacht)
        copy.setName(safeName(original.getName()));

        // Scopes tief kopieren (Maps)
        if (original.getBefore() != null) copy.getBefore().putAll(original.getBefore());
        if (original.getBeforeEnabled() != null) copy.getBeforeEnabled().putAll(original.getBeforeEnabled());
        if (original.getTemplates() != null) copy.getTemplates().putAll(original.getTemplates());
        if (original.getTemplatesEnabled() != null) copy.getTemplatesEnabled().putAll(original.getTemplatesEnabled());
        if (original.getAfter() != null) copy.getAfter().putAll(original.getAfter());
        if (original.getAfterEnabled() != null) copy.getAfterEnabled().putAll(original.getAfterEnabled());
        if (original.getAfterDesc() != null) copy.getAfterDesc().putAll(original.getAfterDesc());

        // Then (falls noch als Liste genutzt)
        if (original.getThen() != null) copy.getThen().addAll(original.getThen());

        // WHEN-Actions shallow klonen mit neuer ID/Parent
        if (original.getWhen() != null) {
            for (TestAction step : original.getWhen()) {
                TestAction cloned = cloneActionShallowForDuplicate(step, copy.getId());
                copy.getWhen().add(cloned);
            }
        }
        return copy;
    }

    private TestSuite cloneSuiteDeepForDuplicate(TestSuite original, RootNode rootModel) {
        if (original == null) return null;
        TestSuite copy = new TestSuite();
        copy.setId(UUID.randomUUID().toString());
        copy.setParentId(rootModel != null ? rootModel.getId() : null);

        // Name eindeutiger machen
        String baseName = safeName(original.getName());
        copy.setName(uniqueSuiteName("copy of " + baseName));

        // Beschreibung
        copy.setDescription(original.getDescription());

        // Suite-Scopes
        if (original.getBeforeAll() != null) copy.getBeforeAll().putAll(original.getBeforeAll());
        if (original.getBeforeAllEnabled() != null) copy.getBeforeAllEnabled().putAll(original.getBeforeAllEnabled());
        if (original.getBeforeEach() != null) copy.getBeforeEach().putAll(original.getBeforeEach());
        if (original.getBeforeEachEnabled() != null) copy.getBeforeEachEnabled().putAll(original.getBeforeEachEnabled());
        if (original.getTemplates() != null) copy.getTemplates().putAll(original.getTemplates());
        if (original.getTemplatesEnabled() != null) copy.getTemplatesEnabled().putAll(original.getTemplatesEnabled());
        if (original.getAfterAll() != null) copy.getAfterAll().putAll(original.getAfterAll());
        if (original.getAfterAllEnabled() != null) copy.getAfterAllEnabled().putAll(original.getAfterAllEnabled());
        if (original.getAfterAllDesc() != null) copy.getAfterAllDesc().putAll(original.getAfterAllDesc());

        // Then-Liste übernehmen
        copy.getThen().addAll(original.getThen());

        // Cases tief kopieren
        if (original.getTestCases() != null) {
            for (TestCase srcCase : original.getTestCases()) {
                TestCase caseCopy = cloneCaseDeepForDuplicate(srcCase, copy.getId());
                caseCopy.setName(uniqueCaseName(copy, caseCopy.getName()));
                copy.getTestCases().add(caseCopy);
            }
        }
        return copy;
    }

    private void insertSuiteCopyAfter(TestSuite originalSuite, TestSuite newSuite, RootNode rootModel) {
        List<TestSuite> suites = TestRegistry.getInstance().getRoot().getTestSuites();
        int insertIndex = suites.indexOf(originalSuite);
        if (insertIndex < 0) suites.add(newSuite); else suites.add(insertIndex + 1, newSuite);
    }

    private void insertCaseCopyAfter(TestCase originalCase, TestCase newCase, TestSuite parentSuite) {
        List<TestCase> cases = parentSuite.getTestCases();
        int insertIndex = cases.indexOf(originalCase);
        if (insertIndex < 0) cases.add(newCase); else cases.add(insertIndex + 1, newCase);
    }

    private void insertActionCopyAfter(TestAction originalAction, TestAction newAction, TestCase parentCase) {
        List<TestAction> steps = parentCase.getWhen();
        int insertIndex = steps.indexOf(originalAction);
        if (insertIndex < 0) steps.add(newAction); else steps.add(insertIndex + 1, newAction);
    }

    /**
     * CellEditor für in-place Rename von TestAction: Text wird als description übernommen.
     */
    private static class ActionNodeCellEditor extends AbstractCellEditor implements javax.swing.tree.TreeCellEditor {
        // Ersetzter Editor mit Icon-Anzeige und Tastatur-Handling
        private final JTree tree;
        private final JTextField field = new JTextField();
        private final JPanel panel = new JPanel(new BorderLayout(4,0));
        private final JLabel iconLabel = new JLabel();
        private TestNode currentNode;
        private String prevDescription;
        private String prevRenderedLabel;
        private boolean canceled = false;
        ActionNodeCellEditor(JTree tree) {
            this.tree = tree;
            field.setBorder(BorderFactory.createEmptyBorder(1,2,1,2));
            // kleine Innenmargen gegen Abschneiden des ersten Zeichens
            try { field.setMargin(new Insets(0, 2, 0, 2)); } catch (Throwable ignore) {}
            field.setColumns(30); // Grundbreite
            panel.setOpaque(false);
            iconLabel.setOpaque(false);
            panel.add(iconLabel, BorderLayout.WEST);
            panel.add(field, BorderLayout.CENTER);
            // Key Handling für Enter / ESC
            field.addKeyListener(new java.awt.event.KeyAdapter() {
                @Override public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        canceled = false;
                        stopCellEditing();
                    } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                        canceled = true;
                        cancelCellEditing();
                    }
                }
            });
        }
        /**
         * Erzeugt und initialisiert die Editor-Komponente für eine Action-Zelle.
         * @param tree der JTree
         * @param value zu editiender Wert (TestNode)
         * @param sel ob selektiert
         * @param expanded ob expandiert
         * @param leaf ob Blatt
         * @param row Zeilenindex
         * @return Editor-Komponente (Panel mit Icon + Textfeld)
         */
        @Override
        public Component getTreeCellEditorComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row) {
            if (!initFromValue(value)) return new JLabel(String.valueOf(value));

            TestAction action = (TestAction) currentNode.getModelRef();
            initTextFieldsFromAction(action);

            Icon icon = applyRendererIcon(tree, value, expanded, leaf, row);
            sizeEditorToRow(tree, value, icon, expanded, leaf, row);
            deferResizeAndFocus(tree, row);

            canceled = false;
            return panel;
        }

        private boolean initFromValue(Object value) {
            if (!(value instanceof TestNode)) { currentNode = null; return false; }
            currentNode = (TestNode) value;
            Object ref = currentNode.getModelRef();
            if (!(ref instanceof TestAction)) { currentNode = null; return false; }
            return true;
        }

        private void initTextFieldsFromAction(TestAction action) {
            prevDescription = action.getDescription();
            prevRenderedLabel = TestTreeController.renderActionLabel(action);
            String start = (prevDescription != null && !prevDescription.trim().isEmpty())
                    ? prevDescription.trim()
                    : prevRenderedLabel;
            field.setText(start);
        }

        private Icon applyRendererIcon(JTree tree, Object value, boolean expanded, boolean leaf, int row) {
            Component rendComp = tree.getCellRenderer()
                    .getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true);
            Icon ic = null;
            if (rendComp instanceof JLabel) {
                ic = ((JLabel) rendComp).getIcon();
                iconLabel.setIcon(ic);
            } else {
                iconLabel.setIcon(null);
            }
            return ic;
        }

        /**
         * Startet Fokus und selektiert Text, hängt einen Viewport-ChangeListener an zur Breitenanpassung.
         * @param tree JTree
         * @param row Zeile im Baum
         */
        private void deferResizeAndFocus(JTree tree, int row) {
            SwingUtilities.invokeLater(() -> {
                try {
                    TreePath path = tree.getPathForRow(row);
                    if (path == null) return;
                    Rectangle b = tree.getPathBounds(path);
                    if (b == null) return;
                    Rectangle vis = tree.getVisibleRect();
                    int pad = 4;
                    int newW = Math.max(b.width, (vis.x + vis.width) - b.x - pad);
                    int iconWidthLater = 0;
                    Icon cicon = iconLabel.getIcon();
                    if (cicon != null) iconWidthLater = cicon.getIconWidth() + 6;
                    Container p = panel.getParent();
                    if (p != null) {
                        p.setSize(newW, b.height);
                        panel.setPreferredSize(new Dimension(newW, b.height));
                        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, b.height));
                        field.setPreferredSize(new Dimension(Math.max(60, newW - iconWidthLater - 8), b.height));
                        p.revalidate();
                        p.repaint();
                    }
                } catch (Throwable ignore) { }
                field.requestFocusInWindow();
                field.selectAll();
            });
        }

        /**
         * Passt die Editor-Breite an den sichtbaren Viewport an (volle Breite ab Einrückung bis rechts).
         * @param tree JTree
         * @param row Zeilenindex
         */
        private void sizeEditorToRow(JTree tree, Object value, Icon ic, boolean expanded, boolean leaf, int row) {
            Rectangle rb = tree.getRowBounds(row);
            Component rendComp = tree.getCellRenderer()
                    .getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true);
            int iconW = (ic != null) ? ic.getIconWidth() + 6 : 0;
            int prefW = (rb != null ? rb.width : rendComp.getPreferredSize().width);
            int fieldW = Math.max(100, prefW - iconW - 8);
            Dimension d = new Dimension(prefW, rendComp.getPreferredSize().height);
            panel.setPreferredSize(d);
            field.setPreferredSize(new Dimension(fieldW, d.height));
        }

        /**
         * Liefert den finalen Wert nach Editierung. Setzt Beschreibung oder entfernt sie bei leerem Eingabefeld.
         * Persistiert Änderungen und feuert ein Update-Event.
         * @return neuer Label-Wert
         */
        @Override
        public Object getCellEditorValue() {
            if (currentNode == null) return null;
            Object ref = currentNode.getModelRef();
            if (!(ref instanceof TestAction)) return currentNode.getUserObject();
            TestAction action = (TestAction) ref;
            if (!canceled) {
                String edited = field.getText() != null ? field.getText().trim() : "";
                if (edited.isEmpty()) {
                    action.setDescription(null); // zurück zu generiertem Label
                } else if (!edited.equals(prevDescription) && !edited.equals(prevRenderedLabel)) {
                    action.setDescription(edited);
                }
            }
            // Bei Abbruch nichts verändert -> Beschreibung bleibt wie zuvor
            currentNode.setUserObject(TestTreeController.renderActionLabel(action));
            if (!canceled) { try { TestRegistry.getInstance().save(); } catch (Throwable ignore) {} }
            javax.swing.tree.TreeModel m = tree.getModel();
            if (m instanceof DefaultTreeModel) ((DefaultTreeModel) m).nodeChanged(currentNode);
            // Statt Selection-Reset: gezielt Event feuern, damit Preview aktualisiert
            if (!canceled) {
                try { de.bund.zrb.event.ApplicationEventBus.getInstance().publish(new TestActionUpdatedEvent(action)); } catch (Throwable ignore) {}
            }
            return currentNode.getUserObject();
        }
        /**
         * Entfernt den Viewport-Listener beim Ende oder Abbruch der Editierung um Leaks zu vermeiden.
         */
        @Override public boolean stopCellEditing() { super.stopCellEditing(); return true; }
        @Override public void cancelCellEditing() { super.cancelCellEditing(); }
        /**
         * Prüft ob eine Zelle editierbar ist (nur Doppelklick oder F2 auf TestAction).
         * @param e Input-Event
         * @return true wenn editierbar
         */
        @Override
        public boolean isCellEditable(java.util.EventObject e) {
            // Maus-Doppelklick wie gehabt
            if (e instanceof java.awt.event.MouseEvent) {
                java.awt.event.MouseEvent me = (java.awt.event.MouseEvent) e;
                if (me.getClickCount() < 2) return false; // nur Doppelklick
                TreePath path = tree.getPathForLocation(me.getX(), me.getY());
                if (path == null) return false;
                Object comp = path.getLastPathComponent();
                return comp instanceof TestNode && ((TestNode) comp).getModelRef() instanceof TestAction;
            }
            // Programmgesteuert (F2/startEditingAtPath) oder andere Events: prüfe aktuelle Selektion
            TreePath sel = tree.getSelectionPath();
            if (sel == null) return false;
            Object comp = sel.getLastPathComponent();
            return comp instanceof TestNode && ((TestNode) comp).getModelRef() instanceof TestAction;
        }
        @Override public boolean shouldSelectCell(java.util.EventObject e) { return true; }
    }
}
